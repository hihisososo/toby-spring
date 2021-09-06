<h2>5장(서비스 추상화)</h2>
<h3>5.1 사용자 레벨 관리 기능 추가</h3>
* 지금까지 만들었던 DAO 는 CRUD 같은 기초적인 기능만 가능하다.
여기에 사용자 레벨 관리 기능을 추가해보자.
  
* 기능 상세는 아래와 같다.
    1. 사용자의 레벨은 BASIC, SILVER, GOLD 세 가지 중 하나다.
    2. 사용자가 처름 가입하면 BASIC 레벨이 되며, 이후 활동에 따라서 한 단계씩 업그레이드 될 수 있다.
    3. 가입 후 50회 이상 로그인을 하면 BASIC에서 SILVER 레벨이 된다.
    4. SILVER 레벨이면서 30번 이상 추천을 받으면 GOLD 레벨이 된다.
    5. 사용자 레벨의 변경 작업은 일정한 주기를 가지고 일괄적으로 진행된다,.
    변경 작업 전에는 조건을 충족하더라도 레벨의 변경이 일어나지 않는다.
       
<h4>5.1.1 필드 추가</h4>
* User 클래스에 사용자의 레벨을 저장할 필드를 추가하면 아래와 같다.
```java
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private static final int BASIC = 1;
    private static final int SILVER = 1;
    private static final int GOLD = 1;
    
    int level;
    String id;
    String name;
    String password;
}
```

* 이렇게 선언했을 때의 문제는, int 값이기 때문에 다른 int 정보를 넣는
  user.setLevel(other.getSum()) 과 같은 실수를 해도 컴파일러가 체크해주지 못한다는 점이다.
  
* 숫자 타입을 직접 사용하는 것보다는 enum 을 이용하는 것이 안전하고 편하다.
```java
package springbook.user.domain;

public enum Level {
    BASIC(1), SILVER(2), GOLD(3);

    private final int value;

    Level(int value) {
        this.value = value;
    }

    public int intValue() {
        return value;
    }

    public static Level valueOf(int value) {
        switch (value) {
            case 1:
                return BASIC;
            case 2:
                return SILVER;
            case 3:
                return GOLD;
            default:
                throw new AssertionError("Unknown value: " + value);
        }
    }
}
```
* 이렇게 만든 Level 타입의 변수를 User 클래스에 추가하고, 로그인 횟수와 추천수도 추가하면 아래와 같다.
```java
public class User {
    ...
    Level level;
    int login;
    int recommend;
}
```
* DB 테이블에도 Level, login, recommend 필드를 추가한다.
* UserDaoTest 도 수정하면 아래와 같다.
```java
public class UserDaoTest {
    ...
    @BeforeEach
    public void setUp() {
        this.user1 = new User("gyumee", "박성철", "springno1", Level.BASIC, 1, 0);
        this.user2 = new User("leegw700", "이길원", "springno2", Level.SILVER, 55, 10);
        this.user3 = new User("bumjin", "박범진", "springno3", Level.GOLD, 100, 40);
    }
    ...
    @Test
    public void addAndGet() {
      dao.deleteAll();
      assertThat(dao.getCount(), is(0));
  
      dao.add(user1);
      dao.add(user2);
      assertThat(dao.getCount(), is(2));
  
      User userget1 = dao.get(user1.getId());
      checkSameUser(userget1, user1);
  
      User userget2 = dao.get(user2.getId());
      checkSameUser(userget2, user2);
    }
    ...
    private void checkSameUser(User user1, User user2) {
        assertThat(user1.getId(), is(user2.getId()));
        assertThat(user1.getName(), is(user2.getName()));
        assertThat(user1.getPassword(), is(user2.getPassword()));
        assertThat(user1.getLevel(), is(user2.getLevel()));
        assertThat(user1.getLogin(), is(user2.getLogin()));
        assertThat(user1.getRecommend(), is(user2.getRecommend()));
  }
```

* UserDaoJdbc 도 수정하면 아래와 같다.
```java
public class UserDaoJdbc implements UserDao {
    private RowMapper<User> userMapper = new RowMapper<User>() {
        @Override
        public User mapRow(ResultSet resultSet, int i) throws SQLException {
            User user = new User();
            user.setId(resultSet.getString("id"));
            user.setName(resultSet.getString("name"));
            user.setPassword(resultSet.getString("password"));
            user.setLevel(Level.valueOf(resultSet.getInt("level")));
            user.setLogin(resultSet.getInt("login"));
            user.setRecommend(resultSet.getInt("recommend"));

            return user;
        }
    };
    ...
    public void add(final User user) {
        this.jdbcTemplate.update("insert into users (id,name,password,level,login,recommend) values(?,?,?,?,?,?)", user.getId(), user.getName(), user.getPassword(), user.getLevel().intValue(), user.getLogin(), user.getRecommend());
    }
    ...
```

<h4>5.1.2 사용자 수정 기능 추가</h4>
* 사용자 수정 기능을 추가하기 위해 테스트를 먼저 만들면 아래와 같다.
```java
@Test
    public void update(){
        dao.deleteAll();

        dao.add(user1);

        user1.setName("오민규");
        user1.setPassword("springno6");
        user1.setLevel(Level.GOLD);
        user1.setLogin(1000);
        user1.setRecommend(999);
        dao.update(user1);

        User user1update = dao.get(user1.getId());
        checkSameUser(user1, user1update);
    }
```
* 테스트의 update 메소드를 구현하기 위해 UserDao 와 UserDaoJdbc 를 수정하면 아래와 같다.
```java
public interface UserDao {
    ...
    void update(User user);
}

public class UserDaoJdbc implements UserDao {
  ...
  public void update(User user) {
    this.jdbcTemplate.update("update users set name = ?, password = ?, level = ?, login = ?, recommend = ? where id = ?", user.getName(), user.getPassword(), user.getLevel().intValue(), user.getLogin(), user.getRecommend(), user.getId());
  }
  ...
}
```

* 테스트를 정상적으로 돌려보면 성공하는데, 만약 update sql 에 where 조건이 빠졌다면? 그래도 정상 동작인것처럼 보일 것이다.
* 해결하기 위해 보완하려면, update 의 리턴값을 확인하거나(영향을 받은 row 개수가 리턴), 두개의 데이터를 이용하여 원하는 
데이터만 업뎃되었는지 확인하는 방법이 있다.
  
* 두번째 방법을 이용하여 검증하면 아래와 같다.

```java
@Test
    public void update(){
        dao.deleteAll();

        dao.add(user1);
        dao.add(user2);

        user1.setName("오민규");
        user1.setPassword("springno6");
        user1.setLevel(Level.GOLD);
        user1.setLogin(1000);
        user1.setRecommend(999);
        dao.update(user1);

        User user1update = dao.get(user1.getId());
        checkSameUser(user1, user1update);
        User user2same = dao.get(user2.getId());
        checkSameUser(user2, user2same);
    }
```

<h4>5.1.3 UserService.upgradeLevels()</h4>
* 이전의 작업을 통해 User 테이블의 CRUD 작업은 검증되었다.
* 사용자 레벨 업그레이드 로직은 어디에 두어야 할까?
* DAO 는 데이터를 조작하는 부분일 뿐이므로 적절하지 않음, 비즈니스 로직을 위한 UserService 를 생성한다.
```java
package springbook.user.service;

import springbook.user.dao.UserDao;

public class UserService {
    UserDao userDao;

    public void setUserDao(UserDao userDao) {
        this.userDao = userDao;
    }
}
```

```java
package springbook.user.dao;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import springbook.user.service.UserService;

import javax.sql.DataSource;

@SpringBootConfiguration
public class BeanFactory {
    @Bean
    public UserDao userDao() {
        UserDaoJdbc userDao = new UserDaoJdbc();
        userDao.setDataSource(dataSource());
        return userDao;
    }
    @Bean
    public DataSource dataSource(){
        SimpleDriverDataSource dataSource = new SimpleDriverDataSource();

        dataSource.setDriverClass(org.h2.Driver.class);
        dataSource.setUrl("jdbc:h2:file:~/test;AUTO_SERVER=TRUE");
        dataSource.setUsername("sa");
        dataSource.setPassword("");
        return dataSource;
    }

    @Bean
    public UserService userService(){
        UserService service = new UserService();
        service.setUserDao(userDao());
        return service;
    }
}
```

```java
package springbook.user.dao;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import springbook.user.service.UserService;

import javax.sql.DataSource;

@SpringBootConfiguration
public class TestBeanFactory {
    @Bean
    public UserDao userDao() {
        UserDaoJdbc userDao = new UserDaoJdbc();
        userDao.setDataSource(dataSource());
        return userDao;
    }

    @Bean
    public DataSource dataSource() {
        SimpleDriverDataSource dataSource = new SimpleDriverDataSource();

        dataSource.setDriverClass(org.h2.Driver.class);
        dataSource.setUrl("jdbc:h2:file:~/testdb;AUTO_SERVER=TRUE");
        dataSource.setUsername("sa");
        dataSource.setPassword("");
        return dataSource;
    }

    @Bean
    public UserService userService(){
        UserService service = new UserService();
        service.setUserDao(userDao());
        return service;
    }
}

```

* 테스트 클래스에서, DI 받을 수 있도록 아래와 같이 수정해준다.
```java
@SpringBootTest
@ContextConfiguration(classes = {TestBeanFactory.class})
public class UserServiceTest {
  @Autowired
  UserService userService;
}
```

* 간단한 테스트 메소드를 추가하면 아래와 같다.
```java
    @Test
    public void bean(){
        assertThat(userService, is(notNullValue()));
    }
```

* 사용자 레벨을 업데이트하는 비즈니스 로직을 구현해보면 아래와 같다.
```java
public void upgradeLevels() {
        List<User> users = userDao.getAll();
        for (User user : users) {
            Boolean changed = null;
            if (user.getLevel() == Level.BASIC && user.getLogin() >= 50) {
                user.setLevel(Level.SILVER);
                changed = true;
            } else if (user.getLevel() == Level.SILVER && user.getRecommend() >= 30) {
                user.setLevel(Level.GOLD);
                changed = true;
            } else if (user.getLevel() == Level.GOLD) {
                changed = false;
            } else {
                changed = false;
            }

            if (changed) {
                userDao.update(user);
            }
        }
    }
```
* 비즈니스 로직을 만들었으니, 테스트를 구현해보면 아래와 같다.
```java
public class UserServiceTest {
    @Autowired
    UserService userService;

    @Autowired
    UserDao userDao;

    List<User> users;

    @BeforeEach
    public void setUp() {
        users = Arrays.asList(
                new User("bumjin", "박범진", "p1", Level.BASIC, 49, 0),
                new User("joytouch", "강명성", "p2", Level.BASIC, 50, 0),
                new User("erwins", "신승한", "p3", Level.SILVER, 60, 29),
                new User("madnite1", "이상호", "p4", Level.SILVER, 60, 30),
                new User("green", "오민규", "p5", Level.GOLD, 100, 100)
        );
    }
    @Test
    public void bean() {
        assertThat(userService, is(notNullValue()));
    }

    @Test
    public void upgradeLevels() {
        userDao.deleteAll();
        for (User user : users) {
            userDao.add(user);
        }

        userService.upgradeLevels();

      checkLevelUpgraded(users.get(0), Level.BASIC);
      checkLevelUpgraded(users.get(1), Level.SILVER);
      checkLevelUpgraded(users.get(2), Level.SILVER);
      checkLevelUpgraded(users.get(3), Level.GOLD);
      checkLevelUpgraded(users.get(4), Level.GOLD);
    }

    private void checkLevelUpgraded(User user, Level expectedLevel) {
        User userUpdate = userDao.get(user.getId());
        assertThat(userUpdate.getLevel(), is(expectedLevel));
    }
}
```

* 테스트를 돌려보면 성공했음을 확인할 수 있다.

<h4>5.1.4 UserService.add()</h4>
* 초기 가입자의 Level 이 Basic 으로 설정될 수 있도록 로직을 추가한다.
* UserDao 는 입력 데이터의 액세스에만 관심을 가져야 하므로 UserService 에 추가한다.
* 일단 테스트를 먼저 만들어서, 초기 사용자 레벨이 null 일 경우와, 있을 경우를 테스트 하는 코드를 만든다.
```java
    @Test
    public void add() {
        userDao.deleteAll();

        User userWithLevel = users.get(4);
        User userWithoutLevel = users.get(0);
        userWithoutLevel.setLevel(null);

        userService.add(userWithLevel);
        userService.add(userWithoutLevel);

        User userWithLevelRead = userDao.get(userWithLevel.getId());
        User userWithoutLevelRead = userDao.get(userWithoutLevel.getId());

        assertThat(userWithLevelRead.getLevel(), is(userWithLevel.getLevel()));
        assertThat(userWithoutLevelRead.getLevel(), is(Level.BASIC));
    }
```

* UserService 에 아래와 같이 add() 메소드를 만들고 테스트해본다.
```java
    public void add(User user) {
        if (user.getLevel() == null) user.setLevel(Level.BASIC);
        userDao.add(user);
    }
```

<h4>5.1.5 코드 개선</h4>
* 업그레이드 로직은 생성되었으나, 코드를 작성한 후 항상 재 확인해야 할 것이 있다. 
  1.  코드의 중복된 부분
  2.  코드가 무엇을 하는지 이해하기 어렵지 않은가?
  3.  코드가 자신이 있어야 할 자리에 있는가?
  4.  앞으로 변경이 일어난다면 어디에 일어날 수 있고, 변화에 쉽게 대응할 수 있게 작성되어 있는가?
  
* updgradeLevels() 메소드의 문제점은 if/else 문이 읽기 불편하며, boolean 변수로 업데이트 여부를
판단하여 업데이트 하는 것도 가독성이 좋지 않다.
  
* if/else 문 안에서, 업데이트 판단여부와 레벨판단을 같이 하고 있어서(두가지 일을 하고 있음) 가독성이 떻어진다.
* 리팩토링을 위해 가장 추상적인 레벨에서 로직을 생각해서 코드를 짜면 아래와 같다.
```java
    public void upgradeLevels(){
        List<User> users = userDao.getAll();
        for(User user : users){
            if(canUpgradeLevel(user)){
                upgradeLevel(user);
            }
        }
    }
```

* 추상적으로 깔끔하게 분리가 되었으므로, 이 부분을 메소드를 통해 구현하면 된다.
```java
private void upgradeLevel(User user) {
        if (user.getLevel() == Level.BASIC) user.setLevel(Level.SILVER);
        else if (user.getLevel() == Level.SILVER) user.setLevel(Level.GOLD);
        userDao.update(user);
    }

    private boolean canUpgradeLevel(User user) {
        Level currentLevel = user.getLevel();
        switch (currentLevel) {
            case BASIC:
                return (user.getLogin() >= 50);
            case SILVER:
                return (user.getRecommend() >= 30);
            case GOLD:
                return false;
            default:
                throw new IllegalArgumentException("Unknown Level : " + currentLevel);
        }
    }
```

* upgradeLevel() 메소드의 문제점은 아래와 같다.
  1. 다음 레벨에 대한 정보가 노골적으로 드러나 있음
  2. 없는 레벨에 대한 에러 처리가 없음
  
* 해당 부분을 리팩토링하기 위해 Level enum 에서 담당하도록 추가해준다.
```java
package springbook.user.domain;

public enum Level {
    GOLD(3, null), SILVER(2, GOLD), BASIC(1, SILVER);

    private final int value;
    private final Level next;

    Level(int value, Level next) {
        this.value = value;
        this.next = next;
    }

    public int intValue() {
        return value;
    }

    public Level nextLevel(){
        return this.next;
    }

    public static Level valueOf(int value) {
        switch (value) {
            case 1:
                return BASIC;
            case 2:
                return SILVER;
            case 3:
                return GOLD;
            default:
                throw new AssertionError("Unknown value: " + value);
        }
    }
}

```
* 다음 레벨에 대한 정보는 enum 에서 처리하도록 하였으므로, 사용자 정보가 바뀌는 부분을 User 객체로 옮기면 아래와 같다.

```java
package springbook.user.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User {
    ...
    public void upgradeLevel(){
        Level nextLevel = level.nextLevel();
        if(nextLevel == null){
            throw new IllegalStateException(this.level + "은 업그레이드가 불가능합니다");
        }else{
            this.level = nextLevel;
        }
    }
}

```
* 해당 작업을 통해 업그레이드 메소드가 아래와 같이 간결해진다.
```java
    private void upgradeLevel(User user) {
        user.upgradeLevel();
        userDao.update(user);
    }
```

* 각각의 객체에게 자신에 종속된 데이터를 변경하게 하는 부분을 통해 코드가 깔끔해졌다(객체지향 설계)
* User 객체에 업그레이드 메소드가 생겼으므로 테스트 코드를 추가하면 아래와 같다.
```java
package springbook.user.service;
...
public class UserTest {
    User user;

    @BeforeEach
    public void setUp() {
        user = new User();
    }

    @Test
    public void upgradeLevel() {
        Level[] levels = Level.values();
        for (Level level : levels) {
            if (level.nextLevel() == null) continue;
            user.setLevel(level);
            user.upgradeLevel();
            assertThat(user.getLevel(), is(level.nextLevel()));
        }
    }

    @Test
    public void cannotUpgradeLevel() {
        Level[] levels = Level.values();
        for (Level level : levels) {
            if (level.nextLevel() != null) continue;
            user.setLevel(level);
            assertThrows(IllegalStateException.class, () -> {
                user.upgradeLevel();
            });
        }
    }
}

```
* 변경된 부분을 통해 UserServiceTest 리팩토링 및 비교 변수 값 상수화를 시키면 아래와 같다.
```java
package springbook.user.service;

import springbook.user.dao.UserDao;
import springbook.user.domain.Level;
import springbook.user.domain.User;

import java.util.List;

public class UserService {
    public static final int MIN_LOGOUT_FOR_SILVER = 50;
    public static final int MIN_RECOMMEND_FOR_GOLD = 30;
    ...
    private boolean canUpgradeLevel(User user) {
        Level currentLevel = user.getLevel();
        switch (currentLevel) {
            case BASIC:
                return (user.getLogin() >= MIN_LOGOUT_FOR_SILVER);
            case SILVER:
                return (user.getRecommend() >= MIN_RECOMMEND_FOR_GOLD);
            case GOLD:
                return false;
            default:
                throw new IllegalArgumentException("Unknown Level : " + currentLevel);
        }
    }
}

```
```java
public class UserServiceTest {
    ...
    @BeforeEach
    public void setUp() {
        users = Arrays.asList(
                new User("bumjin", "박범진", "p1", Level.BASIC, MIN_LOGCOUNT_FOR_SILVER - 1, 0),
                new User("joytouch", "강명성", "p2", Level.BASIC, MIN_LOGCOUNT_FOR_SILVER, 0),
                new User("erwins", "신승한", "p3", Level.SILVER, MIN_RECOMMEND_FOR_GOLD - 1, 29),
                new User("madnite1", "이상호", "p4", Level.SILVER, MIN_RECOMMEND_FOR_GOLD, 30),
                new User("green", "오민규", "p5", Level.GOLD, 100, Integer.MAX_VALUE)
        );
    }
    ...
```
* 추가적으로, 정책이 기간별로 바뀔 수 있으므로, 인터페이스화 하여 DI 받아 적용할 수 있다.

<h3>5.2 트랜잭션 서비스 추상화</h3>
<h4>5.2.1 모 아니면 도</h4>
* 사용자 레벨 업그레이드 중, 중간에 에러가 발생했다면 어떻게 될까? 누구는 업그레이드 되고, 누구는 업그레이드 되지 않았다면
최악의 상황 아닐까? 차라리 모두 업데이트 되지 않고 나중에 일괄처리하는 부분이 나을 것이다.
  
* 일단 해당과 같은 상황을 재현하기 위해 일부러 에러를 발생시키는 코드를 만들어야 한다.
* 중간에 에러를 발생시키려면 어떻게 구현하여야 할까?
* UserService 를 상속하여 오버라이딩 하여 구현하면 아래와 같다.
```java
public class UserService {
    ...
    protected void upgradeLevel(User user) {
        user.upgradeLevel();
        userDao.update(user);
    }
    ...
```
```java
public class UserServiceTest {
    ...
    static class TestUserService extends UserService {
        private String id;

        private TestUserService(String id) {
            this.id = id;
        }

        @Override
        protected void upgradeLevel(User user) {
            if (user.getId().equals(this.id)) {
                throw new TestUserServiceException();
            }
            super.upgradeLevel(user);
        }
    }

    static class TestUserServiceException extends RuntimeException {
    }
```
* 위의 소스를 통하여 업그레이드 중 중간에 강제 에러 발생시키는 테스트는 아래와 같다.
```java
@Test
    public void upgradeAllOrNothing(){
        UserService testUserService = new TestUserService(users.get(3).getId());
        testUserService.setUserDao(this.userDao);
        userDao.deleteAll();
        for(User user : users){
            userDao.add(user);
        }

        try{
            testUserService.upgradeLevels();
            fail("TestUserServiceException expected");
        }catch(TestUserServiceException e){

        }

        checkLevelUpgraded(users.get(1), false);
    }
```
* 해당 테스트를 실행하면, 2번째 사용자의 레벨이 업데이트 되어서, 테스트가 실패하는 것을 알 수 있다.
* 중간에 실패 시, 원자성을 보존할 수 있도록 업데이트 시 트랜잭션을 적용해야 한다.

<h4>5.2.2 트랜잭션 경계설정</h4>
* DB SQL 은 그 자체로 원자성을 지원해준다, 하지만 여러 쿼리를 사용할 때는 원자성을 지원해주지 않는다. 
  원자성을 지원하려면 중간에 쿼리가 실패할 경우 이전 쿼리까지 원복하여야 한다.
이것을 롤백이라 한다.
* 반대로 여러 SQL 이 성공하였을 때 DB 에 작업 성공을 확정지어주는 것을 트랜잭션 커밋이라 한다.
* 현재 jdbcTemplate 상에서는 각 쿼리마다 커넥션 생성 -> 쿼리 실행 -> 커넥션 종료 동작한다, 트랜잭션은 커넥션의 하위 개념이므로
결국에는 UserService 단에서 커넥션을 생성 후, UserDao 까지 파라미터로 넘겨주어야 커넥션 내에서 트랜잭션을 사용할 수 있다.
* 이제까지 Data 액세스 기술에 독립적이기 위해 UserDao 인터페이스를 구현하여 Connection 을 없얬는데, 사용할 수가 없다.
<h4>5.2.3 트랜잭션 동기화</h4>
* Connection 을 파라미터로 전달하는 문제를 해결해보자 upgradeLevels() 메소드가 트랜잭션을 사용하는 것을 피할 수 없다
그렇지만 Dao 인터페이스 상에 Connection 을 제거하고 싶다.
* 이를 위해 스프링에서 트랜잭션 동기화 방법을 제공해준다.
* 트랜잭션 동기화 : Connection 오브젝트를 특정한 저장소에 보관해두고 이후에는 보관한 Connection 을 사용하도록 해주는 것
* JdbcTemplate 이 트랜잭션 동기화 방법을 사용하도록 한다.
* UserService 에 트랜잭션 동기화를 적용하면 아래와 같다.
```java
public void upgradeLevels() throws Exception {
        TransactionSynchronizationManager.initSynchronization();
        Connection c = DataSourceUtils.getConnection(dataSource);
        c.setAutoCommit(false);

        try {
            List<User> users = userDao.getAll();
            for (User user : users) {
                if (canUpgradeLevel(user)) {
                    upgradeLevel(user);
                }
            }
            c.commit();
        }catch(Exception e){
            c.rollback();
            throw e;
        }finally {
            DataSourceUtils.releaseConnection(c,dataSource);
            TransactionSynchronizationManager.unbindResource(this.dataSource);
            TransactionSynchronizationManager.clearSynchronization();
        }
    }
```
* 테스트 코드도 수정하면 아래와 같다.
```java
@Test
    public void upgradeAllOrNothing() throws Exception {
        UserService testUserService = new TestUserService(users.get(3).getId());
        testUserService.setUserDao(this.userDao);
        testUserService.setDataSource(dataSource);
        
        userDao.deleteAll();
        for(User user : users){
            userDao.add(user);
        }

        try{
            testUserService.upgradeLevels();
            fail("TestUserServiceException expected");
        }catch(TestUserServiceException e){

        }

        checkLevelUpgraded(users.get(1), false);
    }
```
* 테스트를 돌리면 성공하게 된다.

<h4>5.2.4 트랜잭션 서비스 추상화</h4>
* 하나의 DB 를 사용한다면 문제가 없지만, 여러개의 DB 를 사용한다면 어떻게 해야 할까?
* 여러 DB 의 통합적인 트랜잭션을 위해 JTA(java transaction API) 를 제공하고 있다.
* UserService 는 JTA, JDBC, JPA 등 트랜잭션 코드가 다른 DB 연결 사용 시, 기술마다 다른 코드를
작성하여야 하는 입장이 되었다.
* 이 부분을 해결하기 위해 각 DB 연결 기술 별 Transaction 추상화 계층을 스프링이 제공한다. 
* UserService 에 적용해보면 아래와 같다.
```java
public void upgradeLevels() {
        PlatformTransactionManager transactionManager = new DataSourceTransactionManager(dataSource);
        TransactionStatus status = transactionManager.getTransaction(new DefaultTransactionDefinition());

        try {
            List<User> users = userDao.getAll();
            for (User user : users) {
                if (canUpgradeLevel(user)) {
                    upgradeLevel(user);
                }
            }
            transactionManager.commit(status);
        }catch(RuntimeException e){
            transactionManager.rollback(status);
            throw e;
        }
    }
```
* PlatformTransactionManager 라는 추상화된 인터페이스를 통해 DI 를 이용하여 DB 연결기술에 종속적이지 않도록 하면 아래와 같다.
```java
public class UserService {
    ...
    private PlatformTransactionManager transactionManager;

    public void setTransactionManager(PlatformTransactionManager transactionManager){
        this.transactionManager = transactionManager;
    }
    ...
    public void upgradeLevels() {
        TransactionStatus status = this.transactionManager.getTransaction(new DefaultTransactionDefinition());

        try {
            List<User> users = userDao.getAll();
            for (User user : users) {
                if (canUpgradeLevel(user)) {
                    upgradeLevel(user);
                }
            }
            this.transactionManager.commit(status);
        }catch(RuntimeException e){
            this.transactionManager.rollback(status);
            throw e;
        }
    }
```
* bean 설정도 아래와 같이 변경해주도록 한다.
```java
public class TestBeanFactory {
    ...
    @Bean
    public UserService userService(){
        UserService service = new UserService();
        service.setUserDao(userDao());
        service.setDataSource(dataSource());
        service.setTransactionManager(transactionManager());

        return service;
    }

    @Bean
    public PlatformTransactionManager transactionManager(){
        return new DataSourceTransactionManager();
    }
```
* 테스트 코드도 손보면 아래와 같다.
```java
public class UserServiceTest {
    ...
    @Autowired
    PlatformTransactionManager transactionManager;
    ...
    @Test
    public void upgradeAllOrNothing() throws Exception {
      UserService testUserService = new TestUserService(users.get(3).getId());
      testUserService.setUserDao(this.userDao);
      testUserService.setDataSource(dataSource);
      testUserService.setTransactionManager(transactionManager);
    ...
```
* 테스트를 실행해보면 정상적으로 성공한다.
<h3>5.3 서비스 추상화와 단일 책임 원칙</h3>
* 서비스를 추상화한다는 것은, 많은 장점을 가지고 있다.
* DB 가 바뀌었다고 해서, Dao 의 로직이 바뀔 필요가 없고, 다른 추상화 처리가 된 로직들도 마찬가지다.
추상화를 통해 기술에 종속되지 않는 코드를 작성할 수 있다.
* 단일 책임 원칙은 하나의 모듈은 한 가지 책임을 가져야 한다는 의미다
UserService 는 사용자 비즈니 로직, UserDao 는 사용자 데이터 액세스 로직이다.
UserService 에 Connectino 의 트랜잭션을 직접 사용하는 코드가 들어 있을 때,
UserService 는 트랜잭션 + 비즈니스 로직의 두 가지 책임을 가지는 안좋은 코드가 되었었다.

<h3>5.4 메일 서비스 추상화</h3>
* 사용자 업그레이드 시, 메일 발송 기능을 추가해야 할 때 어떻게 할 것인가?.
<h4>5.4.1 JavaMail 을 이용한 메일 발송 기능</h4>
* 일단 User 데이터에 email 을 추가하고 Dao 및 테스트 코드를 수정한다.
* 자바 메일 발송 시 자바 표준인 JavaMail 을 통하여 발송하면 된다. upgradeLevel() 메소드에서 
발송하도록 하면 아래와 같다.
```java
    protected void upgradeLevel(User user) {
        user.upgradeLevel();
        userDao.update(user);
        sendUpgradeMail(user);
    }

    private void sendUpgradeMail(User user) {
        Properties props = new Properties();
        props.put("mail.smtp.host", "mail.ksug.org");
        Session s = Session.getInstance(props, null);

        MimeMessage message = new MimeMessage(s);
        try {
            message.setFrom(new InternetAddress("useradmin@ksug.org"));
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(user.getEamil()));
            message.setSubject("Upgrade 안내");
            message.setText("사용자님의 등급이 " + user.getLevel().name() + "로 업그레이드 되었습니다");

            Transport.send(message);
        } catch (AddressException e) {
            throw new RuntimeException(e);
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
    }
```
<h4>5.4.2 JavaMail 이 포함된 코드의 테스트</h4>
