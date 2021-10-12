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
* 해당 sql 제공 기능을 분리해본다

