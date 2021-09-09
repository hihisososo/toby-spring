<h2>6장(AOP)</h2>
<h3>6.1 트랜잭션 코드의 분리</h3>
*  UserService 의 비즈니스 로직에 트랜잭션 코드가 한 뭉텅이가 들어가 있어 찜찜하다.
<h4>6.1.1 메소드 분리</h4>
* upgradeLevels() 메소드를 다시 살펴보면, 비즈니스 로직과 트랜잭션에 서로 주고받는 정보가 없다.
* 메소드 추출기법을 통해 비즈니스 로직과 트랜잭션 코드를 분리하면 아래와 같다.
```java
   public void upgradeLevels() {
        TransactionStatus status = this.transactionManager.getTransaction(new DefaultTransactionDefinition());

        try {
            upgradeLevelsInternal();
            this.transactionManager.commit(status);
        } catch (RuntimeException e) {
            this.transactionManager.rollback(status);
            throw e;
        }
    }

    private void upgradeLevelsInternal(){
        List<User> users = userDao.getAll();
        for(User user : users){
            if(canUpgradeLevel(user)){
                upgradeLevel(user);
            }
        }
    }
```
<h4>6.1.2 DI를 이용한 클래스의 분리</h4>
* 트랜잭션 코드가 비즈니스 로직에 아직 자리잡고 있다. 없앨 방법은 없을까?
* 클래스 밖으로 뽑아내 DI 를 적용하면 어떨까?
* UserService 오브젝트를 인터페이스가 없어서 어떤것을 추가하기에는 강한 결합상태이므로, 인터페이스화 하고
기존 로직은 UserServiceImpl 에, 그리고 UserService 를 구현한 UserSericeTx 를 만들어서 
 UserService 를 DI 받아 트랜잭션이 필요한 코드는 트랜잭션을 걸어주고, 아닌 코드는 UserService 에 위임한다.
* 위의 로직을 구현한 코드는 아래와 같다.
```java
public interface UserService {
    public void add(User user);
    public void upgradeLevels();
}
```
```java
public class UserServiceImpl implements UserService {
    ...
    public void upgradeLevels() {
        List<User> users = userDao.getAll();
        for (User user : users) {
            if (canUpgradeLevel(user)) {
                upgradeLevel(user);
            }
        }
    }
```
```java
public class UserServiceTx implements UserService{
    private UserService userService;

    private PlatformTransactionManager transactionManager;

    public void setUserService(UserService userService) {
        this.userService = userService;
    }
    public void setTransactionManager(PlatformTransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

    @Override
    public void add(User user) {
        this.userService.add(user);
    }

    @Override
    public void upgradeLevels() {
        TransactionStatus status = this.transactionManager.getTransaction(new DefaultTransactionDefinition());

        try {
            userService.upgradeLevels();
            this.transactionManager.commit(status);
        } catch (RuntimeException e) {
            this.transactionManager.rollback(status);
            throw e;
        }
    }
}
```
* 그에 맞게 설정파일도 변경하면 아래와 같다.
```java
    @Bean
    public UserService userService() {
        UserServiceTx userServiceTx = new UserServiceTx();
        userServiceTx.setTransactionManager(transactionManager());
        userServiceTx.setUserService(userServiceImpl());

        return userServiceTx;
    }
    ...
    @Bean
    UserServiceImpl userServiceImpl() {
        UserServiceImpl userServiceImpl = new UserServiceImpl();
        userServiceImpl.setUserDao(userDao());
        userServiceImpl.setMailSender(mailSender());
        return userServiceImpl;
    }
```
* 테스트도 분리한 것에 맞게 변경해주면 아래와 같다.
```java
public class UserServiceTest {
    @Autowired
    UserService userService;

    @Autowired
    UserServiceImpl userServiceImpl;
    ...
 
    static class TestUserService extends UserServiceImpl {
    ...
    }
    @Test
    public void upgradeAllOrNothing() throws Exception {
        TestUserService testUserService = new TestUserService(users.get(3).getId());
        testUserService.setUserDao(this.userDao);
        testUserService.setMailSender(mailSender);

        UserServiceTx txUserService = new UserServiceTx();
        txUserService.setTransactionManager(transactionManager);
        txUserService.setUserService(testUserService);

        userDao.deleteAll();
        for (User user : users) {
            userDao.add(user);
        }

        try {
            txUserService.upgradeLevels();
            fail("TestUserServiceException expected");
        } catch (TestUserServiceException e) {

        }

        checkLevelUpgraded(users.get(1), false);
    }
    ...
```
* 해당과 같이 분리하면 이제 비즈니스 로직 작성시에는 트랜잭션과 같은 기술적인 내용에는 전혀 신경쓰지 않아도 된다.
* 또한, 비즈니스 로직에 대한 테스트를 손쉽게 만들어 낼 수 있다.
<h3>6.2 고립된 단위 테스트</h3>
* 테스트 단위는 작을수록 좋다. 여러가지 오브젝트가 얽힌 환경에서의 테스트는 원인 파악이 굉장히 힘들다.
* 테스트 대상이 여러 오브젝트와 환경에 의존하고 있다면 작은 단위의 테스트가 주는 장점을 얻기 힘들다.

<h4>6.2.1 복잡한 의존관계 속의 테스트</h4>
* UserService 는 굉장히 작은 로직을 수행하고 있다. 인터페이스화 하지 않았다면 테스트 시 DB,Mail 서버가 구축되어 있어야 할 것이다.
* 이처럼 환경과 밀접하게 연관된 테스트는 많은 부작용을 초래한다.

<h4>6.2.2 테스트 대상 오브젝트 고립시키기</h4>
* 이처럼 환경과 분리 시키기 위해 UserDao 도 목 오브젝트로 변환시켜야 한다.
* 목 오브젝트를 생성하면 아래와 같다.
```java
static class MockUserDao implements UserDao {
        private List<User> users;
        private List<User> updated = new ArrayList<>();

        private MockUserDao(List<User> users) {
            this.users = users;
        }

        public List<User> getUpdated() {
            return this.updated;
        }

        @Override
        public void update(User user) {
            updated.add(user);
        }

        @Override
        public List<User> getAll() {
            return this.users;
        }

        @Override
        public void deleteAll() {
            throw new UnsupportedOperationException();
        }

        @Override
        public int getCount() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void createTable() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void add(User user) {
            throw new UnsupportedOperationException();
        }

        @Override
        public User get(String id) {
            throw new UnsupportedOperationException();
        }
    }
```
* 테스트도 목 오브젝트를 사용하도록 바꾸어 주면 아래와 같다.
```java
@Test
    public void upgradeLevels() throws Exception {
        UserServiceImpl userServiceImpl = new UserServiceImpl();

        MockUserDao mockUserDao = new MockUserDao(this.users);
        userServiceImpl.setUserDao(mockUserDao);

        MockMailSender mockMailSender = new MockMailSender();
        userServiceImpl.setMailSender(mockMailSender);

        userServiceImpl.upgradeLevels();

        List<User> updated = mockUserDao.getUpdated();
        assertThat(updated.size(), is(2));
        checkUserAndLevel(updated.get(0), "joytouch", Level.SILVER);
        checkUserAndLevel(updated.get(1), "madnite1", Level.GOLD);

        List<String> request = mockMailSender.getRequests();
        assertThat(request.size(), is(2));
        assertThat(request.get(0), is(users.get(1).getEamil()));
        assertThat(request.get(1), is(users.get(3).getEamil()));
    }
```
* 목 오브젝트를 통해, DB 환경에 액세스 하지 않고도 테스트 할 수 있게 되었고 수행 시간에 엄청난 이득을 가져올 수 있게 되었다.

<h4>6.2.3 단위 테스트와 통합 테스트</h4>
* 단위 테스트 : 테스트 대상 클래스를 목 오브젝트 등의 테스트 대역을 이용해 의존 오브젝트나 외부의 리소스를 사용하지 않도록
고립시켜서 테스트 하는 것
* 통합 테스트 : 두개 이상의 성격이나 계층이 다른 오브젝트가 연동하도록 만들어 테스트하거나 또는 외부의 DB나 파일, 서비스등의
리소스가 참여하는 테스트
<h4>6.2.4 목 프레임워크</h4>
* 목 오브젝트를 생성하는 것은 상당히 번거로운 작업이다.
* 목 오브젝트를 편리하게 작성하도록 도와주는 다양한 목 오브젝트 지원 프레임워크가 있다.
* 그 중 하나는 Mockito 프레임워크이다

