<h2>2장(테스트)</h2>
<h3>UserDaoTest 다시보기</h3>
<h4>2.1.1 테스트의 유용성</h4>
* 수정사항이 있어 소스를 수정하게되었을 때, 수정된 소스가 정상동작하는지 
체크하기 위해서는 테스트가 꼭 필요하다
  
<h4>2.1.2 UserDaoTest 의 특징</h4>
* 웹 단위의 테스트의 문제점은, 모든 UI 기능이 만들어진 뒤에 테스트가 가능하다는 점이다
기능 테스트 중 웹 페이지 에러가 났을 경우에도 원인파악이 힘들다
  
* 테스트는 수행가능한 작은 단위로 하는것이 좋다. 해당 테스트를 단위 테스트라고 한다.
* 테스트는 또한 자동으로 수행되도록 코드로 만들어지는 것이 중요함(수정 후 테스트의 자동화)

<h4>2.1.3 UserDaoTest 의 문제점</h4>
* 아래의 UserDaoTest 의 문제점은 무엇일까?
```java
public class UserDaoTest {
    public static void main(String[] args) throws SQLException, ClassNotFoundException {
        ApplicationContext context = new AnnotationConfigApplicationContext(DaoFactory.class);
        UserDao dao = context.getBean("userDao",UserDao.class);

        User user = new User();
        user.setId("whiteship");
        user.setName("백기선");
        user.setPassword("married");

        dao.add(user);

        System.out.println(user.getId() + " 등록 성공");

        User user2 = dao.get(user.getId());
        System.out.println(user2.getName());
        System.out.println(user2.getPassword());

        System.out.println(user2.getId() + " 조회 성공");
    }
}
```
1. 출력을 사람 눈으로 확인하여야 한다.
2. main() 문을 통해 클래스당 한 테스트만 할 수 있다

<h3>2.2 UserDaoTest 개선</h3>
<h4>2.2.1 테스트 검증의 자동화</h4>
* 테스트 성공과 실패를 출력을 통해 나눈다. 해당 작업을 통해 눈으로 성공/실패 여부를 판단할 수 있다.
```java
public class UserDaoTest {
    public static void main(String[] args) throws SQLException, ClassNotFoundException {
        ApplicationContext context = new AnnotationConfigApplicationContext(DaoFactory.class);
        UserDao dao = context.getBean("userDao",UserDao.class);

        User user = new User();
        user.setId("whiteship");
        user.setName("백기선");
        user.setPassword("married");

        dao.add(user);

        System.out.println(user.getId() + " 등록 성공");

        User user2 = dao.get(user.getId());
        
        if(!user.getName().equals(user2.getName())){
            System.out.println("테스트 실패 (name)");
        }else if(!user.getPassword().equals(user2.getPassword())){
            System.out.println("테스트 실패 (password)");
        }else{
            System.out.println("조회 테스트 성공");
        }
    }
}
```
<h4>2.2.2 테스트의 효율적인 수행과 결과 관리</h4>
* 테스트의 편의성을 위해 이미 java 에서는 JUnit 테스트 툴을 지원하고 있다.
* JUnit 을 통해 테스트 코드로 변경한 부분은 아래와 같다
```java

import org.junit.jupiter.api.Test;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
...

@SpringBootTest
public class UserDaoTest {
    @Test
    public void addAndGet() throws SQLException {
        ApplicationContext context = new AnnotationConfigApplicationContext(DaoFactory.class);
        UserDao dao = context.getBean("userDao",UserDao.class);

        User user = new User();
        user.setId("gyumee");
        user.setName("박성철");
        user.setPassword("springno1");

        dao.add(user);

        User user2 = dao.get(user.getId());

        assertThat(user2.getName(), is(user.getName()));
    }
}
```
<h3>2.3 개발자를 위한 테스팅 프레임워크 JUnit</h3>
<h4>JUnit 테스트 실행 방법</h4>
* 기본적으로 JUnit 을 사용하면 main() 문을 통해 JUnit 을 실행시켜줘야 하지만, 
  IDE 의 JUnit 테스트 지원 도구를 사용하면 main() 메소드를 만들지 않아도 된다.
  
<h4>2.3.2 테스트 결과의 일관성</h4>
* 현재 테스트 코드의 문제점은, 실행시마다 DB 데이터를 지운 후 실행해주어야 한다는 점이다.
* 전체삭제(deleteAll), 데이터 개수 조회(getCount) 를 추가한 소스는 아래와 같다.
```java
public void deleteAll() throws SQLException {
        Connection c = dataSource.getConnection();

        PreparedStatement ps = c.prepareStatement("delete from users");

        ps.executeUpdate();

        ps.close();
        c.close();
    }

    public int getCount() throws SQLException {
        Connection c = dataSource.getConnection();

        PreparedStatement ps = c.prepareStatement("select count(*) from users");
        ResultSet rs = ps.executeQuery();
        rs.next();
        int count = rs.getInt(1);

        rs.close();
        ps.close();
        c.close();

        return count;
    }
```
* addAndGet 테스트에 전체삭제 및 전체 개수 카운팅 코드를 적용한다.
```java
    @Test
    public void addAndGet() throws SQLException {
        ...

        dao.deleteAll();
        assertThat(dao.getCount(), is(0));

        User user = new User();
        user.setId("gyumee");
        user.setName("박성철");
        user.setPassword("springno1");

        dao.add(user);
        assertThat(dao.getCount(), is(1));

        User user2 = dao.get(user.getId());

        assertThat(user2.getName(), is(user.getName()));
        assertThat(user2.getPassword(), is(user.getPassword()));
    }
```
* 단위 테스트는 항상 일관성이 있어야 함, 심지어 테스트 실행 순서를 바꾸어도 동일한 결과가 보장되어야 함

<h4>2.3.3 포괄적인 테스트</h4>
* 전체 데이터 개수인 getCount 메소드에 대한 상세한 테스트를 작성한다
* User 클래스에도 파라미터를 받는 생성자를 추가하여 코드를 깔끔하게 해보자
* 추가한 소스는 아래와 같다.
```java
@Test
    public void count() throws SQLException{
        ApplicationContext context = new AnnotationConfigApplicationContext(DaoFactory.class);

        UserDao dao = context.getBean("userDao", UserDao.class);
        User user1 = new User("gyumee","박성철","springno1");
        User user2 = new User("leegw700","이길원","springno2");
        User user3 = new User("bumjin","박범진","springno3");

        dao.deleteAll();
        assertThat(dao.getCount(), is(0));

        dao.add(user1);
        assertThat(dao.getCount(), is(1));

        dao.add(user2);
        assertThat(dao.getCount(), is(2));

        dao.add(user3);
        assertThat(dao.getCount(), is(3));
    }
```
* 어떤 테스트가 먼저 실행될지는 보장되지 않는다. 따라서 독립정으로 동일한 결과를 낼 수 있도록 해야 한다.
* addAndGet 메소드의 테스트도 주어진 id에 해당하는 
  정확한 User 정보를 가져오는지 보완하면 아래와 같다.
  
```java
    @Test
    public void addAndGet() throws SQLException {
        ApplicationContext context = new AnnotationConfigApplicationContext(DaoFactory.class);
        UserDao dao = context.getBean("userDao",UserDao.class);
        User user1 = new User("gyumee", "박성철", "springno1");
        User user2 = new User("leegw700", "이길원", "springno2");

        dao.deleteAll();
        assertThat(dao.getCount(), is(0));

        dao.add(user1);
        dao.add(user2);
        assertThat(dao.getCount(), is(2));

        User userget1 = dao.get(user1.getId());
        assertThat(userget1.getName(), is(user1.getName()));
        assertThat(userget1.getPassword(), is(user1.getPassword()));

        User userget2 = dao.get(user2.getId());
        assertThat(userget2.getName(), is(user2.getName()));
        assertThat(userget2.getPassword(), is(user2.getPassword()));
    }
```
* UserDao 의 get 메소드에서 해당 값이 없을 경우의 동작을 테스트 코드로 어떻게 처리하면 될까?
* 스프링의 EmptyResultDataAccessException 클래스를 이용하여 테스트 코드에서 예외를 기대하도록 아래와 같이 추가해준다.
```java
@Test
    public void getUserFailure() throws SQLException{
        ApplicationContext context = new AnnotationConfigApplicationContext(DaoFactory.class);

        UserDao dao = context.getBean("userDao", UserDao.class);
        dao.deleteAll();
        assertThat(dao.getCount(), is(0));

        assertThrows(EmptyResultDataAccessException.class, () -> {
            dao.get("unkown_id");
        });
    }
```

* 위의 테스트를 실행하면, 데이터를 비우고 가져오려고 하니 SQLException 이 떨어질 것이다. EmptyResultDataAccessException 이 
떨어지기 위해 get 메소드를 아래와 같이 수정한다.
  
```java
public User get(String id) throws SQLException {
        ...

        ResultSet rs = ps.executeQuery();

        User user = null;
        if (rs.next()) {
            user = new User();
            user.setId(rs.getString("id"));
            user.setName(rs.getString("name"));
            user.setPassword(rs.getString("password"));
        }

        rs.close();
        ps.close();
        c.close();

        if (user == null) throw new EmptyResultDataAccessException(1);

        return user;
    }
```
* 위와 같이 적용한 뒤 테스트를 실행해보면 테스트가 성공한 것을 볼 수 있다.
* 보통 개발자들은 되는 부분으로만 테스트를 하는 경향이 있으므로, 부정적인 테스트 케이스를
먼저 만드는 습관을 들이는게 좋다.
  
<h4>2.3.4 테스트가 이끄는 개발</h4>
* 이전 챕터에서 했던 테스트 코드 작성의 특징은, 테스트 코드를 먼저 작성하고 실 동작 코드를 작성했다는 것이다.
* 테스트 코드 선 작성 -> 실 동작 코드 구현 식으로 돌아가는 개발 방식이 존재하고 이를 TDD 라 한다.

<h4>2.3.5 테스트 코드 개선</h4>
* 테스트 코드에도 리팩토링이 필요하며, 현재 코드에서 Bean 생성 부분이 중복되어 나타나고 있다.
* 보통 중복이 되는 부분은 메소드로 빼게 되는데, 이 부분을 JUnit의 @BeforeEach 를 활용하여 코드에 적용하면 아래와 같다.
```java
@SpringBootTest
public class UserDaoTest {
  private UserDao dao;

  @BeforeEach
  public void setUp() {
    ApplicationContext context = new AnnotationConfigApplicationContext(DaoFactory.class);
    dao = context.getBean("userDao", UserDao.class);
  }
  ...
}
```
* 이 @BeforeEach 는 매 테스트 실행 전에 동작하여 dao 를 생성한다.
* 테스트에 필요한 정보나 오브젝트를 픽스처 라고 하며 여기서는 UserDao 및 User 오브젝트가 해당된다.
* User 오브젝트를 @BeforeEach 를 통해 중복 제거하면 아래와 같다.
```java
public class UserDaoTest {
  private UserDao dao;
  private User user1;
  private User user2;
  private User user3;


  @BeforeEach
  public void setUp() {
    ...
    this.user1 = new User("gyumee", "박성철", "springno1");
    this.user2 = new User("leegw700", "이길원", "springno2");
    this.user3 = new User("bumjin", "박범진", "springno3");
  }
  ...
}
```

<h3>2.4 스프링 테스트 적용</h3>
<h4>2.4.1 테스트를 위한 애플리케이션 컨텍스트 관리</h4>
* 위의 @BeforeEach 를 통해 매 테스트마다 ApplicationContext 를 생성하는것은 비효율적이다(무겁기 떄문)
* 테스트의 무결성만 보장한다면, 최초 초기화 1회를 통해 전체가 공유하여 사용하여도 된다.
* @SpringBootTest 어노테이션에서 기본적으로 ContextLoader 제공하고 있다.
해당 부분을 통해 ApplicationContext 를 의존성 주입 시킨 소스로 변경하면 아래와 같다.
  

```java
@SpringBootTest
public class UserDaoTest {
  @Autowired
  ApplicationContext context;
  ...

  @BeforeEach
  public void setUp() {
    dao = context.getBean("userDao", UserDao.class);
    ...
  }
}
```

* 위에서 사용한 @AutoWired 어노테이션에 대해 더 알아보면,
자동으로 맞는 타입의 빈을 찾아 의존성 주입을 시켜주는 어노테이션이다.
* 의존성 주입 시, 생성자나 수정자가 필요하지만 @AutoWired 어노테이션은 필요가 없다
* ApplicationContext 가 빈으로 등록되지 않았는데 주입된 이유는, 기본적으로 스프링은
  ApplicationContext 를 빈으로 등록해 놓기 때문이다.
* 굳이 ApplicationContext 를 주입받을 필요없이 UserDao 빈을 의존성 주입하면 아래와 같다.
```java
@SpringBootTest
@ContextConfiguration(classes = {DaoFactory.class})
public class UserDaoTest {
  @Autowired
  UserDao dao;
  ...
}
```

<h4>2.4.2 DI와 테스트</h4>
* 현재 DataSource 인터페이스를 통해 의존성 주입을 하고 있는데, DataSource 가 바뀔 일이 없을 경우
  인터페이스를 쓰지 않아도 되나?
  
* 인터페이스를 통해 주입하는 것이 좋은 이유
  1. 소프트웨어 개발에서 불변하는 것은 없다.
  2. 추가적인 차원의 서비스 기능을 도입할 수 있음(Connection Count)
  3. 효율적인 테스트를 위해 인터페이스를 적용해야 함(테스트 DB Datasource)
  
* 테스트를 위해 DI 하는 예로는, 아래와 같이 ApplicationContext 의 Datasource 를 테스트 코드 내에서
변경하는 법이 있다.

```java
...
@DirtiesContext
public class UserDaoTest {
  ...

  @BeforeEach
  public void setUp() {
    ...
    DataSource dataSource = new SingleConnectionDataSource("jdbc:h2:file:~/test;AUTO_SERVER=TRUE", "sa", "", true);
    dao.setDataSource(dataSource);
  }
}
```
  
* 위의 @DirtiesContext 어노테이션은, ApplicationContext 를 테스트간 공유하지 않고 매번 새로 생성한다는 뜻이다.
  (UserDao 의 dataSource 가 변경되었으므로 테스트간 공유되지 않아야 하기 때문)
  
* 위처럼 DataSource DI 시, 매번 ApplicationContext 생성이 불편하고 부하가 크다.
* 테스트용 설정클래스를 따로 두는 것을 통해 해결할 수 있음, 아래는 테스트 설정클래스를 통해 구현한 코드이다.
```java
@SpringBootConfiguration
public class TestDaoFactory {
    ...
        dataSource.setUrl("jdbc:h2:file:~/testdb;AUTO_SERVER=TRUE");
       ...
    }
```
```java
...
@ContextConfiguration(classes = {TestDaoFactory.class})
public class UserDaoTest {
    ...
}
```

<h3>2.5 학습 테스트로 배우는 스프링</h3>
<h4>2.5.1 학습 테스트의 장점</h4>
* 학습 테스트란, 자기가 짠 코드에 대한 테스트가 아닌 사용하는 API 나 프레임워크의 기능 테스트를
학습 목적으로 짜는 것을 뜻한다.

* 학습 테스트의 장점은 아래 다섯가지가 있다
  1. 다양한 조건에 따라서, 기능이 어떻게 동작하는지 확인할 수 있다.
  2. 개발중에, 학습 테스트 코드를 참고하여 개발할 수 있다.
  3. 라이브러리, 프레임워크 업데이트 시 호환성 검증을 도와준다
  4. 테스트 작성에 대한 훈련이 됨(테스트가 어려운 애들을 어떻게 쉽게 테스트 케이스를 짤지 연구)
  5. 새로운 기술을 지루하지 않게 배울 수 있다.
  
<h4>2.5.2 학습 테스트 예제</h4>
* JUnit 이 테스트 메소드 수행마다 새로운 오브젝트를 생성하는지에 대한 검증 테스트 코드는 아래와 같다.
```java
package springbook.learningtest.junit;

import org.junit.jupiter.api.Test;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;

public class JUnitTest {
    static JUnitTest testObject;

    @Test
    public void test1(){
        assertThat(this, is(not(sameInstance(testObject))));
        testObject = this;
    }

    @Test
    public void test2(){
        assertThat(this, is(not(sameInstance(testObject))));
        testObject = this;
    }

    @Test
    public void test3(){
        assertThat(this, is(not(sameInstance(testObject))));
        testObject = this;
    }

}
```
* 위 소스의 문제점은, 직전에 만들어진 testObject 와만 비교한다는 것이다. 
* 세개의 testObject 중 어떤것도 중복이 되지 않는다는 것을 검증하도록 아래와 같이 바꾼다.

```java
...
import static org.hamcrest.Matchers.hasItem;

public class JUnitTest {
    static Set<JUnitTest> testObjects = new HashSet<JUnitTest>();

    @Test
    public void test1(){
        assertThat(testObjects, not(hasItem(this)));
        testObjects.add(this);
    }

    @Test
    public void test2(){
        assertThat(testObjects, not(hasItem(this)));
        testObjects.add(this);
    }

    @Test
    public void test3(){
        assertThat(testObjects, not(hasItem(this)));
        testObjects.add(this);
    }
}
```

* 스프링의 ApplicationContext 가 모든 테스트에서 공유되는지 확인하는 테스트는 아래와 같다.
* 각 테스트 별 검증하는 목적은 똑같고, 편한 소스를 사용하면 된다.
```java
...
@ContextConfiguration
public class JUnitTest {
    @Autowired
    ApplicationContext context;

    static Set<JUnitTest> testObjects = new HashSet<JUnitTest>();
    static ApplicationContext contextObject = null;

    @Test
    public void test1() {
        assertThat(testObjects, not(hasItem(this)));
        testObjects.add(this);

        assertThat(contextObject == null || contextObject == this.context, is(true));
        contextObject = this.context;
    }

    @Test
    public void test2() {
        assertThat(testObjects, not(hasItem(this)));
        testObjects.add(this);

        assertTrue(contextObject == null || contextObject == this.context);
        contextObject = this.context;
    }
}
```
<h4>2.5.3 버그 테스트</h4>
* 버그 테스트란, 특정 기능에 문제가 있을 때, 해당 버그를 잘 나타내 줄 수 있는 테스트 코드를 뜻함
  
* 버그 테스트를 만들면 아래와 같은 장점이 있음
  1. 테스트의 완성도를 높여준다(기존의 테스트에 버그 발생 테스트가 추가됨)
  2. 버그의 내용을 명확하게 분석하게 해준다(실패하는 테스트 생성 시 버그파악이 더 명확하게 된다.)
  3. 기술적인 문제를 해결하는데 도움이 된다(코드나 설정을 봐도 원인이 무엇인지 모르겠을 때,
     인터넷 포럼이나 사용자간 에러 공유할 때 도움이 된다.)
     
<h3>2.6 정리</h3>
* 테스트는 자동화 + 빠르게 실행할 수 있어야 함
* 테스트 결과는 실행 순서에 따라서 결과가 달라지면 안됨
* 코드작성과 실행주기가 짧을수록 효율적이다
* 테스트하기 쉬운 코드가 좋은 코드다
* 특정 새로운 기술의 습득을 위해 학습 테스트 코드를 작성할 수 있다.
* 오류에 대한 버그 테스트를 만들어놓으면 유용함

