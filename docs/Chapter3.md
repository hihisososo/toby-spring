## 3장(템플릿)
### 다시 보는 초난감 DAO
#### 3.1.1 예외처리 기능을 갖춘 DAO
* UserDao 소스 코드를 다시 보면, Connection 및 PreparedStatement 를 사용 후 
Exception 발생 시, 리소스 반환이 되지 않음
  
* 리소스 반환을 위해 아래와 같이 메소드를 수정할 수 있다.
```java
...
    public void deleteAll() throws SQLException {
        Connection c = null;
        PreparedStatement ps = null;

        try {
            c = dataSource.getConnection();
            ps = c.prepareStatement("delete from users");
            ps.executeUpdate();
        } catch (SQLException e) {
            throw e;
        } finally {
            if (ps != null) {
                try {
                    ps.close();
                } catch (SQLException e) {
                }
            }
            if (c != null) {
                try {
                    c.close();
                } catch (SQLException e) {
                }
            }
        }
    }

    public int getCount() throws SQLException {
        Connection c = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            c = dataSource.getConnection();
            ps = c.prepareStatement("select count(*) from users");
            rs = ps.executeQuery();
            rs.next();
            return rs.getInt(1);
        } catch (SQLException e) {
            throw e;
        } finally {
            if (ps != null) {
                try {
                    ps.close();
                } catch (SQLException e) {
                }
            }
            if (c != null) {
                try {
                    c.close();
                } catch (SQLException e) {
                }
            }
        }
    }
}
```
### 3.2 변하는 것과 변하지 않는 것
#### 3.2.1 JDBC try/catch/finally 코드의 문제점
* 소스 로직상에는 문제가 없지만, 리소스 반환 구문이 모든 코드에 중복되는 문제가 있음
* 중복되어 나오는 이와같은 로직은 폭탄이 될 가능성이 있음

#### 3.2.2 분리와 재사용을 위핸 디자인 패턴 적용
* Conenction 사용/반납 코드가 현재 중복되어 나타나고 있으므로 템플릿 메소드 패턴(상속)을 이용해 수정하면 아래와 같다.
```java
public class UserDaoDeleteAll extends UserDao {
    protected PreparedStatement makeStatement(Connection c) throws SQLException {
        PreparedStatement ps = c.prepareStatement("delete from users");
        return ps;
    }
}
```
* 위와 같이 상속을 통해 구현하면, 모든 기능마다 상속하여 구현하여야 하는 불편함이 존재한다.
* 템플릿 메소드 패턴보다 유연하고 확성이 뛰어난 전략 패턴을 적용한다.
  (변하는 부분을 여러 클래스로 분리하고, 인터페이스를 통해 사용하는 방식)
  
* 변하는 부분(PreparedStatment 생성) 을 인터페이스화 하면 아래와 같다.
```java
...
public interface StatementStrategy {
    PreparedStatement makePreparedStatement(Connection c) throws SQLException;
}
```
* 이 인터페이스를 통해 삭제 PreparedStatement 생성하는 클래스는 아래와 같다.
```java
...
public class DeleteAllStatement implements StatementStrategy {

    @Override
    public PreparedStatement makePreparedStatement(Connection c) throws SQLException {
        PreparedStatement ps = c.prepareStatement("delete from users");
        return ps;
    }
}
```
* 이 클래스를 UserDao 소스에 적용하면 아래와 같다.
```java
public void deleteAll() throws SQLException {
        ...
        try {
            c = dataSource.getConnection();

            StatementStrategy statementStrategy = new DeleteAllStatement();
            ps = statementStrategy.makePreparedStatement(c);
            ps.executeUpdate();
        } catch (SQLException e) {
        ...
```
* 전략 패턴은 필요에 따라 컨텍스르가 그대로 유지되면서 전략을 바꾸어 쓸 수 있다는 것인데,
  DeleteAllStatement 클래스를 명시적으로 알고 사용하게 되어 있다는 것은 이상함
  
* 전략 패턴은 사용자가 어떤 전략을 사용할지 결정하는게 일반적이다.
* 일전의 UserDaoFactory 가 Connection(전략) 생성 및 UserDao 주입을 맡았던 것처럼
DI 는 전략 패턴의 장점을 일반적으로 확용할 수 있도록 만든 구조이다.
  
* 컨텍스트에 해당하는 부분(불변하는 부분) 을 메소드로 독립시키고, 컨텍스트 메소드에 전략 파라미터를 
받아 호출하도록 하면 아래와 같다.
  
```java
public void deleteAll() throws SQLException {
        StatementStrategy st = new DeleteAllStatement();
        jdbcContextWithStatementStrategy(st);
    }

    public void jdbcContextWithStatementStrategy(StatementStrategy stmt) throws SQLException {
        Connection c = null;
        PreparedStatement ps = null;

        try {
            c = dataSource.getConnection();

            ps = stmt.makePreparedStatement(c);
            ps.executeUpdate();
        } catch (SQLException e) {
        ...
    }
```

### 3.3 JDBC 전략 패턴의 최적화
* 위에서 적용한 전략 패턴을 add() 메소드에 적용하면 아래와 같다.
```java
...
public class AddStatement implements StatementStrategy{
  User user;

  public AddStatement(User user){
    this.user = user;
  }

  @Override
  public PreparedStatement makePreparedStatement(Connection c) throws SQLException {
    PreparedStatement ps = c.prepareStatement("insert into users (id,name,password) values(?,?,?)");
    ps.setString(1, user.getId());
    ps.setString(2, user.getName());
    ps.setString(3, user.getPassword());
    return ps;
  }
}
```
```java
public void add(User user) throws SQLException {
        StatementStrategy st = new AddStatement(user);
        jdbcContextWithStatementStrategy(st);
    }
    ...
    public void deleteAll() throws SQLException {
        StatementStrategy st = new DeleteAllStatement();
        jdbcContextWithStatementStrategy(st);
    }
```
#### 3.3.2 전략과 클라이언트의 동거
* 위와 같이 코드 개선되었지만, 매 전략마다 새로운 클래스를 만들어야 한다는 점,
User 와 같은 부가정보가 필요할 경우 생성자를 만들어 받을 수 있도록 해야 한다는 점이 있다.
  
* 클래스 파일이 많아질 경우는 해당 클래스가 강하게 결합되어 있다면 로컬 클래스를 이용해 줄일 수 있다.
```java
public void add(User user) throws SQLException {
  class AddStatement implements StatementStrategy{
    @Override
    public PreparedStatement makePreparedStatement(Connection c) throws SQLException {
      PreparedStatement ps = c.prepareStatement("insert into users (id,name,password) values(?,?,?)");
      ps.setString(1, user.getId());
      ps.setString(2, user.getName());
      ps.setString(3, user.getPassword());
      return ps;
    }
  }
    StatementStrategy st = new AddStatement();
    jdbcContextWithStatementStrategy(st);
}
```
* 조금 더 욕심을 내서 익명 내부 클래스(이름이 없는 클래스)로 변경하면 아래와 같다.
```java
    public void add(final User user) throws SQLException {
        jdbcContextWithStatementStrategy(new StatementStrategy() {
@Override
public PreparedStatement makePreparedStatement(Connection c) throws SQLException {
        PreparedStatement ps = c.prepareStatement("insert into users (id,name,password) values(?,?,?)");
        ps.setString(1, user.getId());
        ps.setString(2, user.getName());
        ps.setString(3, user.getPassword());
        return ps;
        }
        });
}
```
* deletAll() 메소드도 아래와 같이 변경할 수 있다.
```java
public void deleteAll() throws SQLException {
        StatementStrategy st = new DeleteAllStatement();
        jdbcContextWithStatementStrategy(new StatementStrategy() {
            @Override
            public PreparedStatement makePreparedStatement(Connection c) throws SQLException {
                PreparedStatement ps = c.prepareStatement("delete from users");
                return ps;
            }
        });
    }
```

### 3.4 컨텍스트와 DI
#### 3.4.1 JdbcContext의 분리
* 전략 패턴의 구조로 보자면, UserDao 의 메소드가 클라이언트이고, 익명 내부 클래스가 전략, jdbcContextWithStatementStrategy 는 컨텍스트이다.  
* 컨텍스트 메소드는 변하지 않으므로, 모든 클래스에서 사용할 수 있도록 
UserDao 에서 독립시켜본다.
  
* 컨텍스트 를 독립시키려면, 안에서 사용하는 Connection 객체가 필요함, DI 받을 수 있도록 해준다.
* 분리한 클래스는 아래와 같다
```java
package springbook.user.dao;
...
public class JdbcContext {
    private DataSource dataSource;

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void workWithStatementStrategy(StatementStrategy stmt) throws SQLException {
        Connection c = null;
        PreparedStatement ps = null;

        try {
            c = this.dataSource.getConnection();

            ps = stmt.makePreparedStatement(c);

            ps.executeUpdate();
        } catch (SQLException e) {
            throw e;
        } finally {
            if (ps != null) {
                try {
                    ps.close();
                } catch (SQLException e) {
                }
            }
            if (c != null) {
                try {
                    c.close();
                } catch (SQLException e) {
                }
            }
        }
    }
}
```
* UserDao 는 이제 JdbcContext 를 DI 받아서 사용할 수 있도록 한다.
```java
...
public class UserDao {
    private JdbcContext jdbcContext;

    public UserDao() {
    }

    public void setJdbcContext(JdbcContext jdbcContext) {
        this.jdbcContext = jdbcContext;
    }

    public void add(final User user) throws SQLException {
        jdbcContext.workWithStatementStrategy(new StatementStrategy() {
            @Override
            public PreparedStatement makePreparedStatement(Connection c) throws SQLException {
                PreparedStatement ps = c.prepareStatement("insert into users (id,name,password) values(?,?,?)");
                ps.setString(1, user.getId());
                ps.setString(2, user.getName());
                ps.setString(3, user.getPassword());
                return ps;
            }
        });
    }
    ...
    public void deleteAll() throws SQLException {
        jdbcContext.workWithStatementStrategy(new StatementStrategy() {
            @Override
            public PreparedStatement makePreparedStatement(Connection c) throws SQLException {
                PreparedStatement ps = c.prepareStatement("delete from users");
                return ps;
            }
        });
    }
    ...
}
```
* 보통의 DI 사용은 인터페이스를 두고 DI 시키지만, JdbcContext 는 불변으로 인터페이스를 구현하지 않았다.
* 바뀐 의존성에 따라서, 먼저 테스트 설정파일을 수정하면 아래와 같다.
```java

@SpringBootConfiguration
public class TestDaoFactory {
    @Bean
    public UserDao userDao() {
        UserDao userDao = new UserDao();
        userDao.setDataSource(dataSource());
        userDao.setJdbcContext(jdbcContext());
        return userDao;
    }
    ...
    @Bean
    public JdbcContext jdbcContext() {
        JdbcContext jdbcContext = new JdbcContext();
        jdbcContext.setDataSource(dataSource());
        return jdbcContext;
    }
}
```
#### 3.4.2 JdbcContext 의 특별한 DI
* JdbcContext 를 DI 시킬 때, 인터페이스를 사용하지 않은 이유는 무엇일까?
* 보통은 인터페이스를 통해 DI 시키는 것이 일반적이나, 꼭 그럴 필요는 없음
  
* JdbcContext 를 굳이 스프링 빈으로 등록하여 UserDao 에 DI 시키는 이유는?
  1. JdbcContext 는 싱글톤인 일종의 서비스 오브젝트로써 여러 Dao 에 Jdbc 컨텍스트 메소드를 제공하는데 의의가 있다.
  2. JdbcContexnt 는 DataSurce 를 스프링으로부터 DI 받아야 하기 떄문이다.
  
### 3.5 템플릿과 콜백
* UserDao, StatementStrategy, JdbcContext 를 이용하여 전략 패턴을 이제까지 구현하였다.
해당 부분은 복잡하지 않으나 일정 작업 흐름 + 자주 변경하여 사용하는 일부분에 적합한 구조임
  
* 전략패턴 + 익명 내부 클래스를 사용한 구조를 템플릿/콜백 패턴이라고 한다.

#### 3.5.1 템플릿/콜백의 동작원리
* 템플릿 : 고정된 작업 흐름을 가진 코드(재사용)
* 콜백 : 템플릿 안에서 호출되는 것을 목적으로 만들어진 오브젝트
* 콜백은, 단일 메소드 인터페이스를 보통 사용한다.(특정 기능을 위해 한번 호출되는 경우가 일반적이기 떄문)
* 클라이언트에서 콜백 오브젝트 생성하여 템플릿 메소드에 전달 -> 템플릿 메소드에서 콜백 실행
-> 콜백에서 클라이언트 변수 참조 및 작업 수행 -> 작업결과를 템플릿에서 받고 메소드 마무리 
  -> 클라이언트에서 템플릿 메소드 결과 확인
* 일반적인 DI 라면, 콜백 오브젝트를 템플릿 클래스에서 DI 받아서 사용하겠지만, 이와 다르게 템플릿/콜백
패턴은 메소드 호출 시, 콜백 오브젝트를 생성하여 넘겨준다는 것이 특징이다.
  
#### 3.5.2 편리한 콜백의 재활용
* 템플릿/콜백 방식은 코드를 반복하여 사용하지 않고 변하는 부분만 간결하게 사용할 수 있어 이점이 있다.
* 하지만, 매번 익명 클래스를 생성하는 코드가 보기에 불편하므로, 수정하면 아래와 같다.

```java
public class JdbcContext {
    ...
    public void executeSql(final String query) throws SQLException {
        workWithStatementStrategy(new StatementStrategy() {
            @Override
            public PreparedStatement makePreparedStatement(Connection c) throws SQLException {
                return c.prepareStatement(query);
            }
        });
    }
}
```

```java
public class UserDao {
    ...
    public void deleteAll() throws SQLException {
        this.jdbcContext.executeSql("delete from users");
    }
    ...
```

#### 3.5.3 템플릿/콜백의 응용
* 자주 반복되는 코드가 있다면, 메소드 추출 -> 인터페이스를 통한 DI -> 템플릿/콜백 패턴을 적용해본다.
* 파일을 열어서 모든 라인의 숫자를 더한 합을 돌려주는 간단한 템플릿/콜백 예를 만들어보면 아래와 같다.
```java
public class Calculator {
  public Integer calcSum(String filepath) throws IOException {
    BufferedReader br = null;

    try {
      br = new BufferedReader(new FileReader(filepath));
      Integer sum = 0;
      String line = null;
      while ((line = br.readLine()) != null) {
        sum += Integer.valueOf(line);
      }
      br.close();
      return sum;
    } catch (IOException e) {
      System.out.println(e.getMessage());
      throw e;
    } finally {
      if (br != null) {
        try {
          br.close();
        } catch (IOException e) {
          System.out.println(e.getMessage());
        }
      }
    }
  }
}
```
```java
public class CalcSumTest {
  @Test
  public void sumOfNumbers() throws IOException {
    Calculator calculator = new Calculator();
    int sum = calculator.calcSum(getClass().getResource("/numbers.txt").getPath());
    assertThat(sum, is(10));
  }
}
```
* 이제 모든 숫자의 곱을 계산하는 기능을 추가해야 한다는 요구가 온다면 어떻게 분리할 것인가?
* BufferedReader 를 가지고 읽어서 처리하는 부분이 달라지므로, 해당 부분을 콜백 오브젝트로 만든다.
```java
public interface BufferedReaderCallback {
    Integer doSomethingWithReader(BufferedReader br) throws IOException;
}
```

* 이에 맞게 콜백 오브젝트를 호출하는 부분을 템플릿 메소드에 만든다.

```java
public class Calculator {
  ...
  public Integer fileReadTemplate(String filepath, BufferedReaderCallback callback) throws IOException {
    BufferedReader br = null;

    try {
      br = new BufferedReader(new FileReader(filepath));
      int ret = callback.doSomethingWithReader(br);
      return ret;
    } catch (IOException e) {
      System.out.println(e.getMessage());
      throw e;
    } finally {
      if (br != null) {
        try {
          br.close();
        } catch (IOException e) {
          System.out.println(e.getMessage());
        }
      }
    }
  }
}
```
* calcSum 메소드를 템플릿 메소드를 호출하도록 수정하면 아래와 같이 구현할 수 있다.
```java
public class Calculator {
    public Integer calcSum(String filepath) throws IOException {
        BufferedReaderCallback sumCallback = new BufferedReaderCallback() {
            @Override
            public Integer doSomethingWithReader(BufferedReader br) throws IOException {
                Integer sum = 0;
                String line = null;
                while ((line = br.readLine()) != null) {
                    sum += Integer.valueOf(line);
                }
                return sum;
            }
        };
        return fileReadTemplate(filepath, sumCallback);
    }
    ...
```
* 변하는 로직에 대한 콜백 인터페이스를 통해 곱하는 로직을 추가하기 전에 테스트를 먼저 만들면 아래와 같다.
```java
public class CalcSumTest {
    Calculator calculator;
    String numFilepath;

    @BeforeEach
    public void setUp() {
        this.calculator = new Calculator();
        this.numFilepath = getClass().getResource("/numbers.txt").getPath();
    }
    ...
    @Test
    public void multiplyOfNumbers() throws IOException {
        assertThat(calculator.calcMultiply(numFilepath), is(24));
    }
}
```
* 테스트를 먼저 만들고 콜백을 통해 구현하면 아래와 같다.
```java
public class Calculator {
    ...
    public Integer calcMultiply(String filepath) throws IOException {
        BufferedReaderCallback multiplyCallback = new BufferedReaderCallback() {
            @Override
            public Integer doSomethingWithReader(BufferedReader br) throws IOException {
                Integer multiply = 1;
                String line = null;
                while ((line = br.readLine()) != null) {
                    multiply *= Integer.valueOf(line);
                }
                return multiply;
            }
        };
        return fileReadTemplate(filepath, multiplyCallback);
    }
    ...
```

* 덧셈 계산과 곱셈 계산의 콜백을 비교해보면 추가적으로 공통점이 있다.
* BufferedReader 를 이용해 한 라인씩 읽어와서 계산하는 부분만 다르다.
* 각 라인의 동작을 구현하도록 콜백 인터페이스를 구현하면 아래와 같다.
```java
public interface LineCallback {
    Integer doSomethisWithLine(String line, Integer value);
}
```
```java
public class Calculator {
    public Integer calcSum(String filepath) throws IOException {
        LineCallback sumCallback = new LineCallback() {
            @Override
            public Integer doSomethisWithLine(String line, Integer value) {
                return value + Integer.valueOf(line);
            }
        };
        return lineReadTemplate(filepath, sumCallback, 0);
    }

    public Integer calcMultiply(String filepath) throws IOException {
        LineCallback multiplyCallback = new LineCallback() {
            @Override
            public Integer doSomethisWithLine(String line, Integer value) {
                return value * Integer.valueOf(line);
            }
        };
        return lineReadTemplate(filepath, multiplyCallback, 1);
    }

    public Integer lineReadTemplate(String filepath, LineCallback callback, int initVal) throws IOException {
        BufferedReader br = null;

        try {
            br = new BufferedReader(new FileReader(filepath));
            Integer res = initVal;
            String line = null;
            while ((line = br.readLine()) != null) {
                res = callback.doSomethisWithLine(line, res);
        ...
    }
}
```

* 콜백의 리턴 타입을 다양하게 하고 싶다면 제네릭스를 이용한 콜백도 아래와 같이 가능하다.
```java
public interface LineCallback<T> {
    T doSomethisWithLine(String line, T value);
}
```
```java
public class Calculator {
    ...   
    public String concatenate(String filepath) throws IOException {
        LineCallback<String> concatenateCallback = new LineCallback<String>() {
            @Override
            public String doSomethisWithLine(String line, String value) {
                return value + line;
            }
        };
        return lineReadTemplate(filepath, concatenateCallback, "");
    }

    public <T> T lineReadTemplate(String filepath, LineCallback<T> callback, T initVal) throws IOException {
        BufferedReader br = null;

        try {
            br = new BufferedReader(new FileReader(filepath));
            T res = initVal;
            String line = null;
            while ((line = br.readLine()) != null) {
                res = callback.doSomethisWithLine(line, res);
            }
            return res;
            ...
```

#### 3.6 스프링의 JdbcTemplate
* 위에서 만든 템플릿/콜백을 스프링에서 JdbcTemplate 로 제공하고 있다.
* UserDao 에서 스프링에서 제공하는 JdbcTemplate 를 사용하도록 하려면 아래와 같이 변경한다.
```java
public class UserDao {
    ...
    private JdbcTemplate jdbcTemplate;
    ...
    public void setDataSource(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.dataSource = dataSource;
    }
    ...
```

#### 3.6.1 update()
* deleteAll() 메소드부터 우선적으로 적용하면 아래와 같다.
```java
    public void deleteAll() throws SQLException {
        this.jdbcTemplate.update("delete from users");
    }
```
* add() 메소드에 대한 구현도 jdbcTemplate 를 통해 쉽게 만들 수 있다.
```java
   public void add(final User user) throws SQLException {
        this.jdbcTemplate.update("insert into users (id,name,password) values(?,?,?)", user.getId(), user.getName(), user.getPassword());
    }
```
#### 3.6.2 queryForInt()
* getCount() 메소드에 jdbcTemplate 를 적용 시, 결과 같을 가져오는 콜백 이 있다.
* ResultSetExtractor 라고 하며 ResultSet 을 전달받는 콜백이다. 해당 콜백을 구현하면 아래와 같다.

```java
public int getCount() throws SQLException {
        return this.jdbcTemplate.query(new PreparedStatementCreator() {
            @Override
            public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
                return connection.prepareStatement("select count(*) from users");
            }
        }, new ResultSetExtractor<Integer>() {
            @Override
            public Integer extractData(ResultSet resultSet) throws SQLException, DataAccessException {
                resultSet.next();
                return resultSet.getInt(1);
            }
        });
    }
}
```
* queryForObject() 메소드를 사용하면 아래와 같이 더 깔끔하게 코드가 가능하다.
```java
    public int getCount() throws SQLException {
        return this.jdbcTemplate.queryForObject("select count(*) from users", Integer.class);
    }
```

#### 3.6.3 queryForObject()
* Integer 값 외에도 queryForObject 를 통해 다양한 타입의 결과 객체를 가져올 수 있다.
* queryForObject 를 사용해서 get() 메소드를 수정하면 아래와 같다.
```java
public User get(String id) throws SQLException {
        return this.jdbcTemplate.queryForObject("select * from users where id = ?", new RowMapper<User>() {
            @Override
            public User mapRow(ResultSet resultSet, int i) throws SQLException {
                User user = new User();
                user.setId(resultSet.getString("id"));
                user.setName(resultSet.getString("name"));
                user.setPassword(resultSet.getString("password"));
                return user;
            }
        }, new Object[]{id});
    }
```

#### 3.6.4 query()
* rowMapper 를 좀더 사용해볼 목적으로, 사용자 리스트 전체를 가져오는 기능을 만들어본다.
* 우선 테스트부터 생성해보면 아래와 같다.

```java
    @Test
    public void getAll() {
        dao.deleteAll();

        dao.add(user1); // Id:gyumee
        List<User> users1 = dao.getAll();
        assertThat(users1.size(), is(1));
        checkSameUser(user1, users1.get(0));

        dao.add(user2); // Id: leegw700
        List<User> users2 = dao.getAll();
        assertThat(users2.size(), is(2));
        checkSameUser(user1, users2.get(0));
        checkSameUser(user2, users2.get(1));

        dao.add(user3); // Id: bumjin
        List<User> users3 = dao.getAll();
        assertThat(users3.size(), is(3));
        checkSameUser(user3, users3.get(0));
        checkSameUser(user1, users3.get(1));
        checkSameUser(user2, users3.get(2));
    }

    private void checkSameUser(User user1, User user2) {
        assertThat(user1.getId(), is(user2.getId()));
        assertThat(user1.getName(), is(user2.getName()));
        assertThat(user1.getPassword(), is(user2.getPassword()));
    }
```
* 테스트를 성공하도록 getAll() 메소드를 구현한다, 여러개의 결과를 가져올 떄는 query() 메소드를 사용한다.
```java
public List<User> getAll() {
        return this.jdbcTemplate.query("select * from users order by id", new RowMapper<User>() {
            @Override
            public User mapRow(ResultSet resultSet, int i) throws SQLException {
                User user = new User();
                user.setId(resultSet.getString("id"));
                user.setName(resultSet.getString("name"));
                user.setPassword(resultSet.getString("password"));
                return user;
            }
        });
    }
```

* 테스트는 성공하였으나, 테스트 중 쿼리 결과가 없을 경우의 동작은 테스트 하지 않았다.
* query() 메소드를 쿼리 결과가 없으면 size 0 인 List 를 리턴한다.
* 이 부분을 이용해서 데이터가 없는 경우에 대한 검증 코드를 추가한 테스트는 아래와 같다.
```java
@Test
    public void getAll() {
        dao.deleteAll();

        List<User> users0 = dao.getAll();
        assertThat(users0.size(), is(0));
        ...
```

#### 3.6.5 재사용 가능한 콜백의 분리
* 현재 코드에서 재사용 가능한 부분을 찾아보면, getAll() 과 get() 의 RowMapper 가 중복되어 나타난다.
* 이 부분을 독립시켜 나중에 수정사항이 생겼을 때 한곳에서 처리할 수 있도록 하면 아래와 같다.
```java
public class UserDao {
    private RowMapper<User> userMapper = new RowMapper<User>() {
        @Override
        public User mapRow(ResultSet resultSet, int i) throws SQLException {
            User user = new User();
            user.setId(resultSet.getString("id"));
            user.setName(resultSet.getString("name"));
            user.setPassword(resultSet.getString("password"));
            return user;
        }
    };
    ...
    public User get(String id) {
        return this.jdbcTemplate.queryForObject("select * from users where id = ?", this.userMapper, new Object[]{id});
    }
    ...
    public List<User> getAll() {
        return this.jdbcTemplate.query("select * from users order by id", this.userMapper);
    }

}
```
* 이렇게 모든 메소드를 수정한 UserDao 는 만약 테이블의 수정사항이 있을 때 모두 한 클래스에서 처리할 수 있다(응집도가 높음)
* DB를 무엇을 쓸지에 대해서는 JdbcTemplate 에 맡기고 신경쓰지 않는다.
* userMapper 를 독립된 빈으로 만들거나, sql 문장을 외부 리소스에서 불러오도록 하는 개선사항이 있으나 아직은 개선하지 않는다.

### 3.7 정리
* 예외 발생 가능성 및 공유 리소스의 반환이 필요한 코드는 try/catch/finally 블록으로 관리한다.
* 특정 동작이 고정이고, 일부만 바뀐다면 전략 패턴을 사용해서 처리하면 좋다.
* 단일 전략 메소드를 갖는 전략 패턴이면서, 익명 내부 클래스를 사용해서 매번 전략을 만들고, 컨텍스트 호출과 동시에 전략 DI를 수행하는 방식을
템플릿/콜백 패턴이라고 한다.
  
* 스프링에서는 해당 패턴을 JdbcTemplate 클래스로 제공하고 있다.
