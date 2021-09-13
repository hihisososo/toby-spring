package springbook.user.dao;

import org.springframework.aop.framework.ProxyFactoryBean;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.aop.support.NameMatchMethodPointcut;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.mail.MailSender;
import org.springframework.transaction.PlatformTransactionManager;
import springbook.user.service.*;

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
    public Object userService() throws Exception {
        return proxyFactoryBean().getObject();
    }

    @Bean
    public ProxyFactoryBean proxyFactoryBean() throws Exception {
        ProxyFactoryBean factoryBean = new ProxyFactoryBean();
        factoryBean.setTarget(userServiceImpl());
        factoryBean.setInterceptorNames("transactionAdvisor");

        return factoryBean;
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
    public UserServiceImpl userServiceImpl() {
        UserServiceImpl userServiceImpl = new UserServiceImpl();
        userServiceImpl.setUserDao(userDao());
        userServiceImpl.setMailSender(mailSender());
        return userServiceImpl;
    }

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


}
