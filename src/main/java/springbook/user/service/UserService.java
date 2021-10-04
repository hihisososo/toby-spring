package springbook.user.service;

import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import springbook.user.dao.UserDao;
import springbook.user.domain.Level;
import springbook.user.domain.User;

import javax.sql.DataSource;
import java.util.List;

@Transactional
public interface UserService {
    public void add(User user);
    void deleteAll();
    void update(User user);
    public void upgradeLevels();

    @Transactional(readOnly=true)
    User get(String id);
    @Transactional(readOnly=true)
    public List<User> getAll();


}
