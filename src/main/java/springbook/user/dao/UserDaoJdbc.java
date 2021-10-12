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
            user.setEamil(resultSet.getString("email"));

            return user;
        }
    };

    private JdbcTemplate jdbcTemplate;

    public UserDaoJdbc() {
    }

    public void setDataSource(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

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

    public void createTable() {
        this.jdbcTemplate.update("drop table users");
        this.jdbcTemplate.update("CREATE TABLE users ( id varchar(100), name varchar(100), password varchar(100), level int(9), login int(9), recommend int(9), email varchar(100));");
    }
}

