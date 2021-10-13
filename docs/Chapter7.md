<h2>7장(스프링 핵심 기술의 응용)</h2>
<h3>7.1 SQL 과 DAO 의 분리</h3>
<h4>7.1.1 XML 설정을 이용한 분리</h3>
* sql 을 외부에서 주입하도록 변경하여 설정하면 아래와 같다.
```java
public class BeanFactory {
    @Bean
    public Map<String, String> sqlMap() {
        Map<String, String> sqlMap = new HashMap<String, String>();
        sqlMap.put("add", "insert into users(id, name, password, level, login, recommend, email) values (?,?,?,?,?,?,?)");
        sqlMap.put("get", "select * from users where id = ?");
        sqlMap.put("getAll", "select * from users order by id");
        sqlMap.put("deleteAll", "delete from users");
        sqlMap.put("getCount", "select count(*) from users");
        sqlMap.put("update", "update users set name = ?, password = ?, level = ?, login = ?, recommend = ?, email = ? where id = ?");
        return sqlMap;
    }

    @Bean
    public UserDao userDao() {
        UserDaoJdbc userDao = new UserDaoJdbc();
        userDao.setDataSource(dataSource());
        userDao.setSqlMap(sqlMap());
        return userDao;
    }
...
}
```
```java
package springbook.user.dao;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import springbook.user.domain.Level;
import springbook.user.domain.User;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class UserDaoJdbc implements UserDao {
    private Map<String,String> sqlMap;

    public void setSqlMap(Map<String,String> sqlMap){
        this.sqlMap = sqlMap;
    }
    ...
    public void add(final User user) {
        this.jdbcTemplate.update(sqlMap.get("add"), user.getId(), user.getName(), user.getPassword(), user.getLevel().intValue(), user.getLogin(), user.getRecommend(), user.getEamil());
    }

    public User get(String id) {
        return this.jdbcTemplate.queryForObject(sqlMap.get("get"), this.userMapper, new Object[]{id});
    }

    public void deleteAll() {
        this.jdbcTemplate.update(sqlMap.get("deleteAll"));
    }

    public int getCount() {
        return this.jdbcTemplate.queryForObject(sqlMap.get("getCount"), Integer.class);
    }

    public List<User> getAll() {
        return this.jdbcTemplate.query(sqlMap.get("getAll"), this.userMapper);
    }

    public void update(User user) {
        this.jdbcTemplate.update(sqlMap.get("update"), user.getName(), user.getPassword(), user.getLevel().intValue(), user.getLogin(), user.getRecommend(), user.getEamil(), user.getId());
    }
    ...
}


```
<h4>7.1.2 SQL 제공 서비스</h4>
* DI 를 이용한 sql은 운영 시, 변경할 수도 없고 bean 설정에 SQL 문이 전부 써져있기 떄문에 불편하다
* 해당 sql 제공 기능을 service 를 이용해 분리하면 아래와 같다.
```java
public class BeanFactory {
    ...
    @Bean
    public UserService userService() {
        UserServiceImpl userService = new UserServiceImpl();
        userService.setUserDao(userDao());
        userService.setMailSender(mailSender());
        return userService;
    }
    ...
    @Bean
    public Map<String, String> sqlMap(){
        Map<String, String> sqlMap = new HashMap<String, String>();
        sqlMap.put("add", "insert into users(id, name, password, level, login, recommend, email) values (?,?,?,?,?,?,?)");
        sqlMap.put("get", "select * from users where id = ?");
        sqlMap.put("getAll", "select * from users order by id");
        sqlMap.put("deleteAll", "delete from users");
        sqlMap.put("getCount", "select count(*) from users");
        sqlMap.put("update", "update users set name = ?, password = ?, level = ?, login = ?, recommend = ?, email = ? where id = ?");
        return sqlMap;
    }

    @Bean
    public SqlService sqlService(){
        SimpleSqlService service = new SimpleSqlService();
        service.setSqlMap(sqlMap());
        return service;
    }
```
```java
package springbook.user.dao;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import springbook.user.domain.Level;
import springbook.user.domain.User;
import springbook.user.sqlservice.SqlService;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class UserDaoJdbc implements UserDao {
    private SqlService sqlService;

    public void setSqlService(SqlService sqlService){
        this.sqlService = sqlService;
    }
    ...
    public void add(final User user) {
        this.jdbcTemplate.update(this.sqlService.getSql("add"), user.getId(), user.getName(), user.getPassword(), user.getLevel().intValue(), user.getLogin(), user.getRecommend(), user.getEamil());
    }

    public User get(String id) {
        return this.jdbcTemplate.queryForObject(this.sqlService.getSql("get"), this.userMapper, new Object[]{id});
    }

    public void deleteAll() {
        this.jdbcTemplate.update(this.sqlService.getSql("deleteAll"));
    }

    public int getCount() {
        return this.jdbcTemplate.queryForObject(this.sqlService.getSql("getCount"), Integer.class);
    }

    public List<User> getAll() {
        return this.jdbcTemplate.query(this.sqlService.getSql("getAll"), this.userMapper);
    }

    public void update(User user) {
        this.jdbcTemplate.update(this.sqlService.getSql("update"), user.getName(), user.getPassword(), user.getLevel().intValue(), user.getLogin(), user.getRecommend(), user.getEamil(), user.getId());
    }

```
```java
public interface SqlService {
        String getSql(String key) throws SqlRetrievalFailureException;
}
```
```java
package springbook.user.sqlservice;

public class SqlRetrievalFailureException extends RuntimeException {
    public SqlRetrievalFailureException(String message){
        super(message);
    }

    public SqlRetrievalFailureException(String message, Throwable cause){
        super(message, cause);
    }
}

```
```java
package springbook.user.sqlservice;

import java.util.Map;

public class SimpleSqlService implements SqlService{
    private Map<String, String> sqlMap;

    public void setSqlMap(Map<String, String> sqlMap) {
        this.sqlMap = sqlMap;
    }

    @Override
    public String getSql(String key) throws SqlRetrievalFailureException {
        String sql = sqlMap.get(key);
        if(sql == null){
            throw new SqlRetrievalFailureException(key + " 에 대한 SQL 을 찾을 수 없습니다.");
        }else{
            return sql;
        }
    }
}

```

<h3>7.2 인터페이스의 분리와 자기참조 빈</h3>
<h4>7.2.1 XML 파일 매핑</h4>
* Bean 설정파일 안에 SQL 을 넣어놓고 사용하는 것은 바람직하지 못하다
* xml 파일을 통해 분리해낼 수 있도록 한다, 객체 <-> XMl 간 매핑을 지원해주는 JAXB 를 사용한다.
