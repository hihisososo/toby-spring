package springbook.user.dao;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.mail.MailSender;
import org.springframework.oxm.Unmarshaller;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import springbook.user.service.DummyMailSender;
import springbook.user.service.TestUserService;
import springbook.user.service.UserService;
import springbook.user.service.UserServiceImpl;
import springbook.user.sqlservice.*;
import springbook.user.sqlservice.updatable.EmbeddedDbSqlRegistry;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

import static org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType.H2;

@SpringBootConfiguration
@PropertySource("classpath:application-test.properties")
@EnableTransactionManagement
public class BeanFactory {
    @Value("${datasource.url}")
    String datasourceUrl;

    @Value("${datasource.username}")
    String datasourceUsername;

    @Value("${datasource.password}")
    String datasourcePassword;

    @Value("${datasource.sqlfileName}")
    String sqlfileName;

    @Bean
    public UserDao userDao() {
        UserDaoJdbc userDao = new UserDaoJdbc();
        userDao.setDataSource(dataSource());
        userDao.setSqlService(sqlService());
        return userDao;
    }

    @Bean
    public DataSource dataSource() {
        SimpleDriverDataSource dataSource = new SimpleDriverDataSource();
        dataSource.setDriverClass(org.h2.Driver.class);
        dataSource.setUrl(datasourceUrl);
        dataSource.setUsername(datasourceUsername);
        dataSource.setPassword(datasourcePassword);
        return dataSource;
    }

    @Bean
    public PlatformTransactionManager transactionManager() {
        return new DataSourceTransactionManager(dataSource());
    }

    @Bean
    public MailSender mailSender() {
        return new DummyMailSender();
    }

    @Bean
    public UserService userService() {
        UserServiceImpl userService = new UserServiceImpl();
        userService.setUserDao(userDao());
        userService.setMailSender(mailSender());
        return userService;
    }

    @Bean
    public UserService testUserService() {
        TestUserService testUserService = new TestUserService();
        testUserService.setUserDao(userDao());
        testUserService.setMailSender(mailSender());
        return testUserService;
    }

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }

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
    public SqlService sqlService() {
        OxmSqlService sqlProvider = new OxmSqlService();
        sqlProvider.setUnmarShaller(unmarshaller());
        sqlProvider.setSqlmap(new ClassPathResource("/sqlmap.xml"));
        sqlProvider.setSqlRegistry(embeddedDbSqlRegistry());
        sqlProvider.loadSql();
        return sqlProvider;
    }

    @Bean
    public Unmarshaller unmarshaller(){
        Jaxb2Marshaller unMarshaller = new Jaxb2Marshaller();
        unMarshaller.setContextPath("springbook.user.sqlservice.jaxb");
        return unMarshaller;
    }

    @Bean
    public SqlRegistry sqlRegistry(){
        return new ConcurrentHashMapRegistry();
    }

    @Bean
    public EmbeddedDatabase embeddedDatabase(){
        EmbeddedDatabase db = new EmbeddedDatabaseBuilder()
                .setType(H2)
                .addScript("/schema.sql")
                .build();
        return db;
    }

    @Bean
    public EmbeddedDbSqlRegistry embeddedDbSqlRegistry(){
        EmbeddedDbSqlRegistry sqlRegistry = new EmbeddedDbSqlRegistry();
        sqlRegistry.setDataSource(embeddedDatabase());
        return sqlRegistry;
    }


}
