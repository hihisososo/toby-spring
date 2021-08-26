<h2>1장(오브젝트와 의존 관계)</h2>
<h3>1.1 초난감 DAO</h3>
<h4>1.1.1 User</h4>
* 자바빈 규약을 따르는 User 객체를 아래와 같이 생성한다.
    > 자바빈 : 디폴트 생성자, getter, setter 를 가진 오브젝트 
```java
package springbook.user.domain;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class User {
    String id;
    String name;
    String password;
}
```
<h4>1.1.2 UserDao</h4>
* 사용자 정보를 DB에 넣고 관리할 수 있는 DAO 클래스를 생성한다
```java
package springbook.user.dao;

import springbook.user.domain.User;
import java.sql.*;

public class UserDao {
    public void add(User user) throws ClassNotFoundException, SQLException {
        Class.forName("org.h2.driver");
        Connection c = DriverManager.getConnection("jdbc:h2:file:~/test;AUTO_SERVER=TRUE");

        PreparedStatement ps = c.prepareStatement("insert into users (id,name,password) values(?,?,?)");
        ps.setString(1, user.getId());
        ps.setString(2, user.getName());
        ps.setString(3, user.getPassword());

        ps.executeUpdate();

        ps.close();
        c.close();
    }

    public User get(String id) throws ClassNotFoundException, SQLException {
        Class.forName("org.h2.driver");
        Connection c = DriverManager.getConnection("jdbc:h2:file:~/test;AUTO_SERVER=TRUE");
        PreparedStatement ps = c.prepareStatement("select * from users where id = ?");
        ps.setString(1, id);

        ResultSet rs = ps.executeQuery();
        rs.next();
        User user = new User();
        user.setId(rs.getString("id"));
        user.setName(rs.getString("name"));
        user.setPassword(rs.getString("password"));

        rs.close();
        ps.close();
        c.close();

        return user;
    }
}
``` 
<h4>1.1.3 main() 을 이용한 DAO 테스트 코드</h4>
* main() 메서드를 통해 아래와 같은 테스트 코드 생성한다.
```java
package springbook.user.dao;

import springbook.user.domain.User;
import java.sql.SQLException;

public class UserDaoTest {
    public static void main(String[] args) throws SQLException, ClassNotFoundException {
        UserDao dao = new UserDao();

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

<h3>1.2 DAO 의 분리</h3>
<h4>1.2.1 관심사의 분리</h4>
* 프로그램은 외부 요구사항에 의해 항상 변경되기 마련이다.
* 요구사항에 의해 변경되는 부분은 여러 부분이 한번에 변경되지 않는다, 보통 한 부분에 대해서 변경 요구가 생긴다.
* 변경되는 부분에 필요한 작업을 최소화 하고 다른곳에 문제를 일으키게 하지 않으려면?
* 관심사의 분리를 통해 관심사가 같은 부분은 모으고, 다른 부분은 분리한다.

<h4>1.2.2 커넥션 만들기의 추출<h4>
* UserDao 의 add() 메서드는 세개의 관심사가 있음
    1. Connection 생성
    2. Statement 생성 및 실행
    3. 리소스 close
    
* 커넥션 생성 코드가 두개의 메서드에 중복되어 나타나 있으므로, 아래와 같이 단일 메서드로 묶는다
```java
private Connection getConnection() throws  ClassNotFoundException, SQLException{
        Class.forName("org.h2.driver");
        Connection c = DriverManager.getConnection("jdbc:h2:file:~/test;AUTO_SERVER=TRUE");
        return c;
    }
```
<h4>1.2.3 DB 커넥션 만들기의 독립</h4>
* UserDao 의 Connection 이 환경에 맞게 바뀌어야 할 떄 분리할 수 있는 방법은?
* 다음과 같이 Connection 을 추상 메서드로 만들고 상속 클래스에서 입맛에 맞게 구현한다.(팩토리 메서드 패턴)
```java
  public abstract Connection getConnection() throws ClassNotFoundException, SQLException;

    class NUserDao extends UserDao {
        @Override
        public Connection getConnection() throws ClassNotFoundException, SQLException {
            //N 사 생성 코드
            return null;
        }
    }

    class DUserDao extends UserDao {
        @Override
        public Connection getConnection() throws ClassNotFoundException, SQLException {
            //D 사 생성 코드
            return null;
        }
    }

```
* 상속을 통한 관심사의 분리는 아래와 같은 불편함이 있음
  * 이미 상속을 이용하고 있는 클래스는 적용하지 못함
  * 상속을 통해 구현하면, 부모 <-> 자식 클래스가 밀접하게 관련되어짐
  
<h3>1.3 DAO 의 확장</h3>
<h4>1.3.1 클래스의 분리</h4>
* 밀접한 관련을 가지고 있는 상속으로 관심을 분리 -> 관심 자체를 클래스로 분리
```java
package springbook.user.dao;
...
public class UserDao {
    private SimpleConnectionMaker simpleConnectionMaker;

    public UserDao() {
        simpleConnectionMaker = new SimpleConnectionMaker();
    }

    public void add(User user) throws ClassNotFoundException, SQLException {
        Connection c = simpleConnectionMaker.makeNewConnection();
        ...
    }

    public User get(String id) throws ClassNotFoundException, SQLException {
        Connection c = simpleConnectionMaker.makeNewConnection();
        ...
     }

}
```

```java
package springbook.user.dao;
...
public class SimpleConnectionMaker {
    public Connection makeNewConnection() throws ClassNotFoundException, SQLException {
        Class.forName("org.h2.Driver");
        Connection c = DriverManager.getConnection("jdbc:h2:file:~/test;AUTO_SERVER=TRUE", "sa", "");
        return c;
    }
}
```
* 클래스를 통해 관심사의 분리는 잘 되었으나, N사나 D사에서 사용하는 Connection 을 외부에서 변경할 수 없음
* UserDao 에 SimpleConnectionMaker 의 인스턴스 생성 코드가 들어가 있음
  
<h4>1.3.2 인터페이스의 도입</h4>
* SimpleConnectionMaker 를 인터페이스를 통해 추상화 시킨다
* 하지만 인터페이스를 통해 추상화 시키더라도 여전히 UserDao 에 ConnectionMaker 의 인스턴스 생성 코드가 들어가 있음

<h4>1.3.3 관계설정 책임의 분리<h4>
* UserDao 가 인터페이스가 아닌 인스턴스 생성 코드까지 떠맡아야 하는 부분을 분리한다
* 생성자를 통해서 ConnectionMaker 인스턴스를 '주입' 받도록 한다
```java
public UserDao(ConnectionMaker connectionMaker) {
        this.connectionMaker = connectionMaker;
}
```
* 해당 ConnectionMaker 인터페이스를 통해, Connection 생성 부분은 완전히 UserDao 와 분리됨

<h4>1.3.4 원칙과 패턴</h4>
* 개방 폐쇄 원칙 : 클래스나 모듈은 확장에는 열려 있어야 하고, 변경에는 닫혀있어야 한다.
  UserDao 는 DB 연결 방법 기능 확장에는 열려 있음, UserDao 의 핵심 로직은 Connection 기능 확장에 영향 받지 않도록 닫혀 있음
* 객체지향 설계 원칙(SOLID)
  * 단일 책임 원칙 : 소프트웨어의 설계 부품(클래스, 함수) 등은 단 하나의 책임을 가져야 한다
  * 개방 폐쇄 원칙 : 클래스나 모듈은 확장에는 열려 있어야 하고, 변경에는 닫혀있어야 한다.
  * 리스코프 치환 원칙 : 자식클래스는 부모클래스에서 가능한 행위를 수행할 수 있어야 한다
  * 의존 역전 원칙 : 의존 관계를 맺을 떄 변화하기 쉬운것보다는, 변화하기 어려운 것에 의존해야 함
  * 인터페이스 분리 원칙 : 한 클래스는 자신이 사용하지 않는 인터페이스는 구현하지 않아야 한다
  
<h3>1.4 제어의 역전</h3>
<h4>1.4.1 오브젝트 팩토리</h4>
* 위에서 만든 설계대로 UserDao 를 사용하면, ConnectionMaker 의 구현체 클래스를 UserDaoTest 지정해줘야 함(불필요하게 떠맡고 있음)
* DaoFactory 팩토리 클래스를 통해 객체 생성 및 의존정 주입을 담당한다.
```java
package springbook.user.dao;

public class DaoFactory {
    public UserDao userDao() {
        ConnectionMaker connectionMaker = new SimpleConnectionMaker();
        UserDao userDao = new UserDao(connectionMaker);
        return userDao;
    }
}

```
* 해당 팩토리 클래스를 통해 UserDao 사용 시 초기화에 대한 것을 신경쓰지 않을 수 있다.

<h4>1.4.2 오트젝트 팩토리의 활용</h4>
* DaoFactory 에 UserDao, AccountDao, MessageDao 등 여러 Dao 생성이 추가될 때 ConnectionMaker 인스턴스 생성 코드의 중복이 일어난다.
```java
package springbook.user.dao;

public class DaoFactory {
  public UserDao userDao() {
    UserDao userDao = new UserDao(new SimpleConnectionMaker());
    return userDao;
  }

  public AccountDao accountDao() {
    AccountDao accountDao = new AccountDao(new SimpleConnectionMaker());
    return accountDao;
  }

  public MessageDao messageDao() {
    MessageDao messageDao = new MessageDao(new SimpleConnectionMaker());
    return messageDao;
  }
}

```

* 해당 중복을 제거하기 위해 메서드로 분리해낸다
```java
package springbook.user.dao;

public class DaoFactory {
    public UserDao userDao() {
        UserDao userDao = new UserDao(connectionMaker());
        return userDao;
    }

    public AccountDao accountDao() {
        AccountDao accountDao = new AccountDao(connectionMaker());
        return accountDao;
    }

    public MessageDao messageDao() {
        MessageDao messageDao = new MessageDao(connectionMaker());
        return messageDao;
    }

    public ConnectionMaker connectionMaker(){
        return new SimpleConnectionMaker();
    }
}

```

<h4>1.4.3 제어권의 이전을 통한 제어관계 역전</h4>
* 기존의 일반적인 프로그램에서는 main() 문 내에서 인스턴스 생성, method 호출, 다음 인스턴스 생성이 반복되어 실행된다.
* 초기 UserDao 클래스를 보면, 클래스 및 메서드 내에서 어떤 인스턴스를 사용할지 결정하고 있음
* 제어의 역전에서는 자기가 어떤 인스턴스를 생성할지 결정하지 않음(외부 주입을 받음), 위임받은 제어 권한을 갖는 오브젝트에서 결정되고 만들어짐
* 대표적인 제어의 역전으로는 프레임워크가 있음, 사용자가 어떤 특정 코드를 구현하면 사용자는 어디서 코드가 호출되고 사용되는지 모르며, 
  프레임워크에서 동작 중에 적절하게 코드를 호출한다.
  
* DaoFactory 에서 UserDao 인스턴스 생성 및 Connection 주입을 관장하고 있으므로,  UserDao 클래스는 DaoFactory 에 수동적인 관계가 됨
해당과 같이 구현하는 것도 제어의 역전이라고 할 수 있음

<h3>1.5 스프링의 IoC</h3>
<h4>1.5.1 오프젝트 팩토리를 이용한 스프링 IoC</h4>
* DaoFactory 를 이제 스프링에서 사용할 수 있도록 하려면, 빈 팩토리에 등록해야 한다.
* 빈 : 스프링에서 제어권을 가지고 직접 관계를 부여하는 오브젝트를 말한다.
* 빈 팩토리 : 빈의 생성과 관계설정을 담당하는 IoC 팩토리
* 어플리케이션 컨텍스트 : 빈 팩토리에서 좀더 확장된, 어플리케이션 전반에 걸친 구성요소 제어 담당하는 IoC 엔진
* 아래와 같이 java 클래스를 통하여 빈 팩토리에 등록할 수 있다.
```java
package springbook.user.dao;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@SpringBootConfiguration
public class DaoFactory {
    @Bean
    public UserDao userDao() {
        UserDao userDao = new UserDao(connectionMaker());
        return userDao;
    }
    @Bean
    public ConnectionMaker connectionMaker(){
        return new SimpleConnectionMaker();
    }
}
```
* 이렇게 등록 후 어플리케이션 컨텍스트를 적용하여 test code 를 수정하면 아래와 같다.
```java
package springbook.user.dao;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import springbook.user.domain.User;
import java.sql.SQLException;

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

<h4>1.5.2 어플리케이션 컨텍스트의 동작방식</h4>
* 어플리케이션 컨텍스트는 기본적으로 BeanFactory 인터페이스를 구현하였음.
  client 에서 bean 생성 요청 시, 설정 정보에 따라 생성된 bean 을 리턴해주는 방식으로 동작한다.
* 기존의 DaoFactory 구현과 비교해서 어플리케이션 컨텍스트를 사용했을 떄 이점은?
  1. 클라이언트에서 구체적인 팩토리 클래스를 알 필요가 없음
  2. 빈 생성뿐만이 아닌 종합적인 IoC 서비스를 제공해줌(후처리, 인터셉팅 등등..)
  3. 빈을 다양한 방식으로 검색할 수 있게 해준다

<h4>1.5.3 스프링 IoC의 용어 정리</h4>
* 빈 : 스프링이 IoC 방식으로 관리하는 오브젝트
* 빈 팩토리 : 빈 등록, 생성, 조회 등의 부가적인 관리를 하는 핵심 컨테이너
* 어플리케이션 컨텍스트 : 빈 팩토리가 확장된 IoC 컨테이너, 빈 팩토리 + 부가 서비스
* 설정정보 : 어플리케이션 컨텍스트가 빈을 생성하기 위해 사용하는 설정 정보
* 컨테이너 : 어플리케이션 컨텍스트나 빈 팩토리를 칭하는 말
* 스프링 프레임워크 : 스프링이 제공하는 모든 기능을 통틀어 칭하는 말

<h3>싱글톤 레지스트리와 오브젝트 스코프</h3>
<h4>1.6.1 싱글톤 레지스트리로서의 애플리케이션 컨텍스트</h4>
* 빈 팩토리에서 생성하는 빈은 기본적으로 싱글톤이다
* 싱글톤인 이유는, 다중 사용자 환경에서의 부하를 줄이기 위함(서비스 오브젝트)
* 자바의 일반적인 싱글톤 생성 소스는 여러 아쉬운 점이 있음(생성자 주입 불가 등등..)
* 스프링에서는 이에 싱글톤 레지스트리를 제공하여 스프링 내에서 싱글톤 관리를 하도록 제공함

<h4>1.6.2 싱글톤과 오브젝트의 상태</h4>
* 싱글톤 오브젝트 사용시, 멀티스레딩 환경을 고려하여 상태정보를 내부에 가지지 않은 무상태 방식으로 만들어야 한다.
* 특히 인스턴스 필드의 값을 변경할 때 주의해야 한다.

<h4>1.6.3 스프링 빈의 스코프</h4>
* 빈의 생성, 존재, 적용되는 범위를 빈의 scope 라고 함, 기본 스코프는 싱글톤이며 해당 scope 외에 필요에 따라
다른 스코프를 지정할 수 있다.
  
<h3>의존관계 주입</h3>
<h4>1.7.1 제어의 역전(IoC)과 의존관계 주입</h4>
* IoC 라는 용어는 폭넓게 사용하고 있기 때문에, 스프링을 IoC 컨테이너라고 칭할 시
스프링에서 제공하는 기능의 특징을 명확하게 설명하지 못함, 따라서 의존관계 주입(DI) 이라는 용어를 통해
  스프링의 특징을 명확하게 나타내는 용어가 고안되었음
  
<h4>1.7.2 런타임 의존관계 설정</h4>
* 의존관계 : A가 B에 의존하고 있다 라는 말은, B의 변경사항이 A의 구성에 영향을 미칠 때를 칭한다.
* UserDao 는 ConnectionMaker 인터페이스에 의존하고 있다, 인터페이스에 의존하게 되면 클래스보다 변경에서 자유롭다.
* 인터페이스를 통해 의존하게 되면 실제 런타임시에 의존하는 인스턴스를 알 수 없게 됨(UML 설계에도 나타나지 않음)
* 런타임시에 실 사용대상인 오브젝트 -> 의존 오브젝트라 함
* 의존관계 주입 : 런타임 시, 클라이언트 오브젝트(사용할 주체)에 의존 오브젝트를 연결해주는 작업을 칭함

<h4>1.7.3 의존관계 검색과 주입</h4>
* 의존관계 검색 : 의존관계 주입을 받지 않고, 런타임시에 빈 팩토리에 의존 빈을 검색하여 가져오는 방식(ApplicationContext.getBean())
* 일반적으로 의존관계 주입이 깔끔하기 때문에 의존관계 주입을 대부분 사용함

<h4>1.7.4 의존관계 주입의 응용</h4>
* 의존관계 주입의 이점 : 인터페이스를 구현한 모든 인스턴스를 사용할 수 있음, 기능 갈아끼우기가 가능함
* 아래와 같이 연결횟수 카운팅 기능이 있는 클래스를 구현하여 적용할 수 도 있음
```java
package springbook.user.dao;

import java.sql.Connection;
import java.sql.SQLException;

public class CountingConnectionMaker implements ConnectionMaker{
    int counter = 0;
    private ConnectionMaker realConnectionMaker;

    public CountingConnectionMaker(ConnectionMaker realConnectionMaker){
        this.realConnectionMaker = realConnectionMaker;
    }


    @Override
    public Connection makeNewConnection() throws ClassNotFoundException, SQLException {
        this.counter++;
        return realConnectionMaker.makeNewConnection();
    }

    public int getCounter(){
        return this.counter;
    }
}

```
```java
package springbook.user.dao;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@SpringBootConfiguration
public class CountingDaoFactory {
    @Bean
    public UserDao userDao() {
        UserDao userDao = new UserDao(connectionMaker());
        return userDao;
    }

    @Bean
    public ConnectionMaker connectionMaker() {
        return new CountingConnectionMaker(realConnectionMaker());
    }

    @Bean
    public ConnectionMaker realConnectionMaker() {
        return new SimpleConnectionMaker();
    }
}

```

<h4>1.7.5 메소드를 이용한 의존관계 주입</h4>
* 의존관계 주입은 생성자를 통해서 뿐만이 아니라 수정자, 일반 메소드를 이용하여 주입할 수 있으며, 일반적으로 수정자를 통해 주입한다.
* UserDao 를 수정자를 통해 DI 하도록 만들면 아래와 같다.
```java
public class UserDao {
  private ConnectionMaker connectionMaker;

  public void setConnectionMaker(ConnectionMaker connectionMaker) {
    this.connectionMaker = connectionMaker;
  }
    ...
}
```
```java
    @Bean
    public UserDao userDao() {
        UserDao userDao = new UserDao();
        userDao.setConnectionMaker(connectionMaker());
        return userDao;
    }
```

<h3>~~1.8 XML 을 이용한 설정~~</h3>
<h3>1.9 정리</h3>
* 책임이 다른 코드를 분리해서 두개의 클래스로 만듬(User CRUD 로직, Connection 생성)
* 전략패턴을 통해 변경되는 로직 -> 인터페이스화
* 개방 폐쇄 원칙을 통해 불필요한 변화가 생기지 않도록 함
* 해당 일련의 과정을 통해 낮은 결합도, 높은 응집도의 코드를 생성함
* IoC 를 통해 오브젝트끼리의 관계를 정해주는 책임을 Factory 에 위임하였음
* 싱글톤 레지스트리를 통해 기존 싱글톤의 단점을 극복할 수 있도록 컨테이너에 적용
* 의존관계 주입을 통해 느슨한 인터페이스만 만들어놓고 런타임시 의존관계를 주입받는 케이스를 확인
* 수정자를 통한 의존관계 주입 방식 확인