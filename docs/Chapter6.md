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
* Mockito 프레임워크를 이용하여 테스트를 작성하면 아래와 같다.

<h3>6.3 다이내믹 프록시와 팩토리 빈</h3>
<h4>6.3.1 프록시와 프록시 패턴, 데코레이터 패턴</h4>
* 현재 트랜잭션을 구현한 구조를 보면, 사용자가 인터페이스를 통해 UserService 를 사용하고 있고 
UserServiceTx 가 명령을 받아서 실제 로직인 UserServiceImpl 을 호출해주고 있다.
* 이렇게 중간에 받아서 위임해주는 클래스를 통해 구현하는 로직을 프록시 라고 한다.
UserServiceTx 가 프록시, UserServiceImpl 이 타깃이 된다.
* 런타임시에 부가 기능을 다이나믹하게 제공하기 위해 프록시를 사용하는 것을 데코레이터 패턴이라고 한다.
* 접근 제어를 위해 사용하는 프록시를 프록시 패턴이라고 한다.

<h4>6.3.2 다이내믹 프록시</h4>
* 프록시를 구현 시, 몇가지 불편한 점이 있다.
 1. 타깃의 인터페이스를 전부 구현하고 위임하는 코드를 작성하기 번거롭다.
 2. 부가기능 코드가 중복될 가능성이 많다. update() 메소드 외에 add() 메소드에도 적용되어야 한다고 하면,
트랜잭션 코드가 중복되게 된다. 여기다 여러개의 트랜잭션 프록시 오브젝트를 사용하게 되면 중복이 더 심해진다.
* 인터페이스 메소드의 구현을 해결하는데 유용한 것이 있는데 JDK 의 다이내믹 프록시이다.
* 다이내믹 프록시를 이용한 프록시를 만들어보면 아래와 같다.
```java
public interface Hello {
    String sayHello(String name);
    String sayHi(String name);
    String sayThankYou(String name);
}
```
```java
public class HelloTarget implements Hello{

    @Override
    public String sayHello(String name) {
        return "Hello " + name;
    }

    @Override
    public String sayHi(String name) {
        return "Hi " + name;
    }

    @Override
    public String sayThankYou(String name) {
        return "Thank You " + name;
    }
}

```
```java
public class ProxyTest {
    @Test
    public void simpleProxy(){
        Hello hello = new HelloTarget();
        assertThat(hello.sayHello("Toby"), is("Hello Toby"));
        assertThat(hello.sayHi("Toby"), is("Hi Toby"));
        assertThat(hello.sayThankYou("Toby"), is("Thank You Toby"));
    }
}
```
```java
public class HelloUpperCase implements Hello{
    Hello hello;

    public HelloUpperCase(Hello hello) {
        this.hello = hello;
    }

    @Override
    public String sayHello(String name) {
        return hello.sayHello(name).toUpperCase();
    }

    @Override
    public String sayHi(String name) {
        return hello.sayHi(name).toUpperCase();
    }

    @Override
    public String sayThankYou(String name) {
        return hello.sayHi(name).toUpperCase();
    }
}
```
* 위 코드는 프록시 적용의 일반적인 문제점 두 가지를 모두 갖고 있다.
모든 메소드를 구현해 위임하도록 코드를 만들어야하고, 부가기능인 UpperCase 가 모든 메소드에 중복되어 나타난다.
* 해결을 위해 다이나믹 프록시를 적용하자, JDK 에서 제공해주는 InvocationHander 인터페이스의 invoke() 메소드를 구현하면,
프록시의 모든 메소드 호출 시, invoke() 메소드를 타게 된다.
 이 부분을 통해 로직 및 위임 구현을 없앨 수 있다. 구현하면 아래와 같다.
```java
public class UpperCaseHandler implements InvocationHandler {
    Hello target;

    public UpperCaseHandler(Hello target) {
        this.target = target;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String ret = (String) method.invoke(target, args);
        return ret.toUpperCase();
    }
}
```
* 이 클래스를 사용하는 프록시를 만들어 보면 아래와 같다.
```java
Hello proxiedHello = (Hello) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class[]{Hello.class},
                new UpperCaseHandler(new HelloTarget()));
```
* 이렇게 다이내믹 프록시를 사용하여 구현하였는데 오히려 이전 코드보다 복잡하고 번거로운 파라미터들이 생겼다. 사용상의 이점은 무엇일까?
* 다이내믹 프록시의 장점은, 인터페이스의 메소드가 많으면 많아질 수록 이점이 드러나는데, 한 개의 메소드에 구현이 정리된다는 것이다.
또한 Mehod 파라미터를 통해 메소드의 여러 부가정보를 통해 로직을 각각 다르게 가져가면, 한데에 모인 깔끔한 코드가 된다.

<h4>6.3.3 다이내믹 프록시를 이용한 트랜잭션 부가기능</h4>
* TxUserService 를 다이내믹 프록시를 이용하여 구현하기위해 InvecationHander 를 구현하면 아래와 같다.
```java
public class TransactionHandler implements InvocationHandler {
    private Object target;
    private PlatformTransactionManager transactionManager;
    private String pattern;

    public void setTarget(Object target) {
        this.target = target;
    }

    public void setTransactionManager(PlatformTransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (method.getName().startsWith(pattern)) {
            return invokeInTransaction(method, args);
        } else {
            return method.invoke(target, args);
        }
    }

    private Object invokeInTransaction(Method method, Object[] args) throws Throwable {
        TransactionStatus status = this.transactionManager.getTransaction(new DefaultTransactionDefinition());

        try {
            Object ret = method.invoke(target, args);
            this.transactionManager.commit(status);
            return ret;
        } catch (InvocationTargetException e) {
            this.transactionManager.rollback(status);
            throw e.getTargetException();
        }
    }
}
```
* 기존의 upgradeAllOrNothing() 메소드를 다이내믹 프록시를 이용하도록 수정하면 아래와 같다.
```java
    @Test
    public void upgradeAllOrNothing() throws Exception {
        ...
        TransactionHandler txHandler = new TransactionHandler();
        txHandler.setTarget(testUserService);
        txHandler.setTransactionManager(transactionManager);
        txHandler.setPattern("upgradeLevels");
        UserService txUserService = (UserService) Proxy.newProxyInstance(getClass().getClassLoader()
                , new Class[]{UserService.class}, txHandler);
        ...
    }
```

<h4>6.3.4 다이내믹 프록시를 위한 팩토리 빈</h4>
* 다이내믹 프록시를 오브젝트는 일반적으로 Bean 으로 등록할 수 없다. 왜냐하면 오브젝트 생성 메소드 자체가 static 이기 떄문이다(xml 설정에만 해당)
따라서 BeanFactory 를 만들어서 설정에 적용해주어야 한다.
* BeanFactory interface 를 구현하여 설정하면 아래와 같다.
```java
public class TestBeanFactory {
    ...
    @Bean
    public Object userService() throws Exception {
     return txProxyFactoryBean().getObject();
    }
   
    @Bean
    public TxProxyFactoryBean txProxyFactoryBean() throws Exception {
     TxProxyFactoryBean txProxyFactoryBean = new TxProxyFactoryBean();
     txProxyFactoryBean.setTarget(userServiceImpl());
     txProxyFactoryBean.setTransactionManager(transactionManager());
     txProxyFactoryBean.setPattern("upgradeLevels");
     txProxyFactoryBean.setServiceInterface(UserService.class);
   
     return txProxyFactoryBean;
    }
 }
```
* 프록시 빈 설정 후, 테스트 코드를 살펴보면 트랜잭션이 검증되는 곳은 UpgradeAllOrNothing() 메소드 밖에 없다
해당 메소드에서도 심지어 Proxy 객체를 생성하여 테스트 하고 있으므로 빈에 등록된 것과는 별개다
* 또한 타깃 오브젝트가 중간에 오류를 낼수 있는 TestUserService 가 아니므로 중간에 오류도 나지 않는다.
* 해당 부분을 개선하기 위한 방법 중 하나로, 팩토리 빈을 직접 가져와서 인스턴스를 생성하는 방식을 차용한다.
* 적용한 테스트 코드는 아래와 같다.
```java
public class UserServiceTest {
    ...
    @Autowired
    ApplicationContext context;
    ...
    @Test
    @DirtiesContext
    public void upgradeAllOrNothing() throws Exception {
        TestUserService testUserService = new TestUserService(users.get(3).getId());
        testUserService.setUserDao(this.userDao);
        testUserService.setMailSender(mailSender);

        TxProxyFactoryBean txProxyFactoryBean = context.getBean("&txProxyFactoryBean", TxProxyFactoryBean.class);
        txProxyFactoryBean.setTarget(testUserService);
        UserService txUserService = (UserService) txProxyFactoryBean.getObject();
        ...
```
<h4>6.3.5 프록시 팩토리 빈 방식의 장점과 한계</h4>
* 이제 트랜잭션을 적용하고 싶은 DAO 가 있으면, 코드 추가 없이 빈 설정으로만 구현할 수 있다.
* 하지만 여러 클래스에 공통적으로 적용하려고 할 시, 설정의 중복이 발생하게 된다.
* 여러개의 부가 기능을 제공하고 싶을 경우에도 프록시 빈 설정이 굉장히 비대해지게 된다.

<h3>6.3 스프링의 프록시 팩토리 빈</h3>
<h4>6.4.1 ProxyFactoryBean</h4>
* 다이나믹 프록시를 편하게 생성하도록, 일관되게 생성하도록 추상화된 ProxyFactoryBean 을 제공한다.
* 일전에 생성했던 프록시 학습 테스트를 ProxyFactoryBean 을 사용하도록 하면 아래와 같다.
```java
public class DynamicProxyTest {

    @Test
    public void proxyFactoryBean(){
        ProxyFactoryBean pfBean = new ProxyFactoryBean();
        pfBean.setTarget(new HelloTarget());
        pfBean.addAdvice(new UppercaseAdvice());

        Hello proxiedHello = (Hello) pfBean.getObject();

        assertThat(proxiedHello.sayHello("Toby"), is("HELLO TOBY"));
        assertThat(proxiedHello.sayHi("Toby"), is("HI TOBY"));
        assertThat(proxiedHello.sayThankYou("Toby"), is("THANK YOU TOBY"));

    }

    static class UppercaseAdvice implements MethodInterceptor{
        public Object invoke(MethodInvocation invocation) throws Throwable{
            String ret = (String)invocation.proceed();
            return ret.toUpperCase();
        }
    }

    static interface Hello {
        String sayHello(String name);
        String sayHi(String name);
        String sayThankYou(String name);
    }

    static class HelloTarget implements Hello {

        @Override
        public String sayHello(String name) {
            return "Hello " + name;
        }

        @Override
        public String sayHi(String name) {
            return "Hi " + name;
        }

        @Override
        public String sayThankYou(String name) {
            return "Thank You " + name;
        }
    }
```
* ProxyFactoryBean 을 사용하면, Hello 라는 타깃 오브젝트가 없어지고, Hello.class 타입도 넘겨주지 않아도 된다.
* addAdvice() 메소드를 통해 부가기능을 여러개 추가할 수 있다. 프록시를 사용해서 처리하는 부가 기능을 advice 라 칭한다.
* pointCut 은, 프록시가 어떤 메소드에 대해 적용될 지 정하는 알고리즘을 뜻한다.
* pointcut, advice 는 타깃 오브젝트 정보가 없으므로, Bean 에 등록하여 DI 를 통해 여러 프록시에 적용할 수 있다.
* pointcut 을 적용하면 아래와 같다.
```java
    @Test
    public void pointcutAdvisor() {
        ProxyFactoryBean pfBean = new ProxyFactoryBean();
        pfBean.setTarget(new HelloTarget());

        NameMatchMethodPointcut pointcut = new NameMatchMethodPointcut();
        pointcut.setMappedName("sayH*");

        pfBean.addAdvisor(new DefaultPointcutAdvisor(pointcut, new UppercaseAdvice()));

        Hello proxiedHello = (Hello) pfBean.getObject();

        assertThat(proxiedHello.sayHello("Toby"), is("HELLO TOBY"));
        assertThat(proxiedHello.sayHi("Toby"), is("HI TOBY"));
        assertThat(proxiedHello.sayThankYou("Toby"), is("Thank You Toby"));

    }
```
* advisor = pointcut + advice 를 뜻한다.

<h4>6.4.2 ProxyFactoryBean 적용</h4>
* JDK 다이내믹 프록시를 사용했던 TxProxyFactoryBean 에서 스프링이 제공하는 ProxyFactoryBean 으로 변경해본다
* 일단 Advice 를 아래와 같이 생성한다.
```java
public class TransactionAdvice implements MethodInterceptor {
    PlatformTransactionManager transactionManager;

    public void setTransactionManager(PlatformTransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        TransactionStatus status = this.transactionManager.getTransaction(new DefaultTransactionDefinition());
        try {
            Object ret = invocation.proceed();
            this.transactionManager.commit(status);
            return ret;
        } catch (RuntimeException e) {
            this.transactionManager.rollback(status);
            throw e;
        }
    }
}
```
* 포인트컷, 어드바이스, 어드바이저, ProxyFactoryBean 을 Bean 에 등록한다.
```java
    @Bean
    public ProxyFactoryBean proxyFactoryBean() throws Exception {
        ProxyFactoryBean factoryBean = new ProxyFactoryBean();
        factoryBean.setTarget(userServiceImpl());
        factoryBean.setInterceptorNames("transactionAdvisor");

        return factoryBean;
    }
    ...
   @Bean
   public TransactionAdvice transactionAdvice() {
           TransactionAdvice transactionAdvice = new TransactionAdvice();
           transactionAdvice.setTransactionManager(transactionManager());
           return transactionAdvice;
           }
   
   @Bean
   public NameMatchMethodPointcut transactionPointcut() {
           NameMatchMethodPointcut pointcut = new NameMatchMethodPointcut();
           pointcut.setMappedName("upgrade*");
           return pointcut;
           }
   
   @Bean
   public DefaultPointcutAdvisor transactionAdvisor() {
           DefaultPointcutAdvisor defaultPointcutAdvisor = new DefaultPointcutAdvisor();
           defaultPointcutAdvisor.setAdvice(transactionAdvice());
           defaultPointcutAdvisor.setPointcut(transactionPointcut());
           return defaultPointcutAdvisor;
           }
```
* 트랜잭션이 연관된 upgradeAllOrNothig() 테스트 메소드를 아래와 같이 수정한다.
```java
@Test
    @DirtiesContext
    public void upgradeAllOrNothing() throws Exception {
        ...
        ProxyFactoryBean txProxyFactoryBean = context.getBean("&proxyFactoryBean", ProxyFactoryBean.class);
        ...
    }
```
<h3>6.5 스프링 AOP</h3>
<h4>6.5.1 자동 프록시 생성</h4>
* ProxyFactoryBean 을 통해 부가기능을 설정파일을 통해 해결할 수 있도록 변경하였다.
* 하지만 이 부가기능이 적용될 클래스가 몇백개라면 설명파일을 만드는 수고가 엄청날 것이다.
* 클래스 리스트를 통해 자동으로 Bean 이 등록되게 할 수 는 없을까?
* DefaultAdvisorAutoProxyCreator Bean 후처리기를 통해, 빈으로 등록 된 모든 어드바이저 내의
포인트컷을 이용하여 프록시 적용 대상인지 판별하여 프록시를 빈으로 등록한다.
* 포인트컷 클래스 필터 테스트를 간단하게 짜보면 아래와 같다.
```java
@Test
    public void classNamePointcutAdvisor() {
        NameMatchMethodPointcut classMethodPointcut = new NameMatchMethodPointcut() {
            public ClassFilter getClassFilter() {
                return new ClassFilter() {
                    @Override
                    public boolean matches(Class<?> clazz) {
                        return clazz.getSimpleName().startsWith("HelloT");
                    }
                };
            }
        };
        classMethodPointcut.setMappedName("sayH*");

        checkAdviced(new HelloTarget(), classMethodPointcut, true);

        class HelloWorld extends HelloTarget{};
        checkAdviced(new HelloWorld(), classMethodPointcut, false);

        class HelloToby extends HelloTarget{};
        checkAdviced(new HelloToby(), classMethodPointcut, true);
    }

    private void checkAdviced(Object target, NameMatchMethodPointcut pointcut, boolean adviced) {
        ProxyFactoryBean pfBean = new ProxyFactoryBean();
        pfBean.setTarget(target);
        pfBean.addAdvisor(new DefaultPointcutAdvisor(pointcut, new UppercaseAdvice()));
        Hello proxiedHello = (Hello) pfBean.getObject();

        if(adviced){
            assertThat(proxiedHello.sayHello("Toby"), is("HELLO TOBY"));
            assertThat(proxiedHello.sayHi("Toby"), is("HI TOBY"));
            assertThat(proxiedHello.sayThankYou("Toby"), is("Thank You Toby"));
        }else{
            assertThat(proxiedHello.sayHello("Toby"), is("Hello Toby"));
            assertThat(proxiedHello.sayHi("Toby"), is("Hi Toby"));
            assertThat(proxiedHello.sayThankYou("Toby"), is("Thank You Toby"));
        }
    }
```

<h4>6.5.2 DefaultAdvisorAutoProxyCreator 의 적용</h4>
* Bean 에 적용하기 위해 클래스 필터가 포함된 포인트컷을 만들면 아래와 같다.
```java
public class NameMatchClassMethodPointcut extends NameMatchMethodPointcut {
    public void setMappedClassName(String mappedClassName){
        this.setClassFilter(new SimpleClassFilter(mappedClassName));
    }

    static class SimpleClassFilter implements ClassFilter{
        String mappedName;

        private SimpleClassFilter(String mappedName){
            this.mappedName = mappedName;
        }

        public boolean matches(Class<?> clazz){
            return PatternMatchUtils.simpleMatch(mappedName, clazz.getSimpleName());
        }
    }
}
```
* DefaultAdvisorAutoProxyCreator 빈을 등록하면, 등록된 빈 중에서 Advisor 인터페이스를 구현한 것을 모두 찾는다.
  DefaultAdvisorAutoProxyCreator 빈을 등록하면 아래와 같다.
```java
   @Bean
    public DefaultAdvisorAutoProxyCreator defaultAdvisorAutoProxyCreator(){
        return new DefaultAdvisorAutoProxyCreator();
    }
```
* 설정파일에 새로 생성한 포인트컷으로 대체하면 아래와 같다
```java
    @Bean
    public NameMatchClassMethodPointcut transactionPointcut() {
        NameMatchClassMethodPointcut pointcut = new NameMatchClassMethodPointcut();
        pointcut.setMappedClassName("*ServiceImpl");
        pointcut.setMappedName("upgrade*");
        return pointcut;
    }
```
* 자동으로 프록시 빈 생성기가 포인트컷을 주입해주므로, 기존에 ProxyFactoryBean 과 관련된 설정은 삭제한다
* UserServiceImpl 도 동일하게 UserService 로 돌아올 수 있도록 한다.
```java
    @Bean
    public UserService userService() {
        UserServiceImpl userService = new UserServiceImpl();
        userService.setUserDao(userDao());
        userService.setMailSender(mailSender());
        return userService;
    }
```

