package springbook.user.dao;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.mail.MailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.transaction.PlatformTransactionManager;
import springbook.user.service.DummyMailSender;
import springbook.user.service.UserService;

import javax.sql.DataSource;

@SpringBootConfiguration
@PropertySource("classpath:application-test.properties")
public class TestBeanFactory {
    @Value("${datasource.url}")
    String datasourceUrl;
    @Value("${datasource.username}")
    String datasourceUsername;
    @Value("${datasource.password}")
    String datasourcePassword;

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
        dataSource.setUrl(datasourceUrl);
        dataSource.setUsername(datasourceUsername);
        dataSource.setPassword(datasourcePassword);
        return dataSource;
    }

    @Bean
    public UserService userService(){
        UserService service = new UserService();
        service.setUserDao(userDao());
        service.setDataSource(dataSource());
        service.setTransactionManager(transactionManager());
        service.setMailSender(mailSender());

        return service;
    }

    @Bean
    public PlatformTransactionManager transactionManager(){
        return new DataSourceTransactionManager(dataSource());
    }

    @Bean
    public MailSender mailSender(){
        return new DummyMailSender();
    }
}
