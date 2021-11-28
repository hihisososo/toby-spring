## 7장(스프링 핵심 기술의 응용)
### 7.1 SQL 과 DAO 의 분리
#### 7.1.1 XML 설정을 이용한 분리
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
#### 7.1.2 SQL 제공 서비스
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

### 7.2 인터페이스의 분리와 자기참조 빈
#### 7.2.1 XML 파일 매핑
* Bean 설정파일 안에 SQL 을 넣어놓고 사용하는 것은 바람직하지 못하다
* xml 파일을 통해 분리해낼 수 있도록 한다, 객체 <-> XMl 간 매핑을 지원해주는 JAXB 를 사용한다.
* 아래와 같은 .xsd 파일을 통해 sql 을 저장할 수 있는 xml 스키마를 선언한다
```xml
<?xml version="1.0" encoding="UTF-8" ?>
<schema xmlns="http://www.w3.org/2001/XMLSchema" targetNamespace="http://www.epril.com/sqlmap"
    xmlns:tns="http://www.epril.com/sqlmap" elementFormDefault="qualified">
    <element name="sqlmap">
        <complexType>
            <sequence>
                <element name="sql" maxOccurs="unbounded" type="tns:sqlType"/>
            </sequence>
        </complexType>
    </element>
    <complexType name="sqlType">
        <simpleContent>
            <extension base="string">
                <attribute name="key" use="required" type="string"/>
            </extension>
        </simpleContent>
    </complexType>
</schema>
```

* jaxb 컴파일러를 통해 컴파일하면 아래와 같이 클래스들이 생성된다
```java

package springbook.user.sqlservice.jaxb;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>anonymous complex type에 대한 Java 클래스입니다.
 * 
 * <p>다음 스키마 단편이 이 클래스에 포함되는 필요한 콘텐츠를 지정합니다.
 * 
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="sql" type="{http://www.epril.com/sqlmap}sqlType" maxOccurs="unbounded"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "sql"
})
@XmlRootElement(name = "sqlmap", namespace = "http://www.epril.com/sqlmap")
public class Sqlmap {

    @XmlElement(namespace = "http://www.epril.com/sqlmap", required = true)
    protected List<SqlType> sql;

    /**
     * Gets the value of the sql property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the sql property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getSql().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link SqlType }
     * 
     * 
     */
    public List<SqlType> getSql() {
        if (sql == null) {
            sql = new ArrayList<SqlType>();
        }
        return this.sql;
    }

}

```
```java

package springbook.user.sqlservice.jaxb;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;


/**
 * <p>sqlType complex type에 대한 Java 클래스입니다.
 * 
 * <p>다음 스키마 단편이 이 클래스에 포함되는 필요한 콘텐츠를 지정합니다.
 * 
 * <pre>
 * &lt;complexType name="sqlType">
 *   &lt;simpleContent>
 *     &lt;extension base="&lt;http://www.w3.org/2001/XMLSchema>string">
 *       &lt;attribute name="key" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/extension>
 *   &lt;/simpleContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "sqlType", namespace = "http://www.epril.com/sqlmap", propOrder = {
    "value"
})
public class SqlType {

    @XmlValue
    protected String value;
    @XmlAttribute(name = "key", required = true)
    protected String key;

    /**
     * value 속성의 값을 가져옵니다.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getValue() {
        return value;
    }

    /**
     * value 속성의 값을 설정합니다.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * key 속성의 값을 가져옵니다.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getKey() {
        return key;
    }

    /**
     * key 속성의 값을 설정합니다.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setKey(String value) {
        this.key = value;
    }

}

```
* 언마샬링 테스트를 위해 아래와 같이 xml 파일을 만든다
```xml
<?xml version="1.0" encoding="UTF-8"?>
<sqlmap xmlns="http://www.epril.com/sqlmap"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:schemaLocation="http://www.epril.com/sqlmap ../../../../../../sqlmap.xsd">
    <sql key="add">insert</sql>
    <sql key="get">select</sql>
    <sql key="delete">delete</sql>
</sqlmap>
```
* 테스트 코드를 아래와 같이 구현해서 언마샬링 테스트한다.
```java
public class JaxbTest {
    @Test
    public void readSqlmap() throws JAXBException, IOException {
        String contextPath = Sqlmap.class.getPackage().getName();
        JAXBContext context = JAXBContext.newInstance(contextPath);

        Unmarshaller unmarshaller = context.createUnmarshaller();

        Sqlmap sqlmap = (Sqlmap) unmarshaller.unmarshal(getClass().getResourceAsStream("sqlmap.xml"));

        List<SqlType> sqlList = sqlmap.getSql();

        assertThat(sqlList.size(), is(3));
        assertThat(sqlList.get(0).getKey(), is("add"));
        assertThat(sqlList.get(0).getValue(), is("insert"));
        assertThat(sqlList.get(1).getKey(), is("get"));
        assertThat(sqlList.get(1).getValue(), is("select"));
        assertThat(sqlList.get(2).getKey(), is("delete"));
        assertThat(sqlList.get(2).getValue(), is("delete"));
    }
```

#### 7.2.2 XML 파일을 이용하는 SQL 서비스
* xml 파일을 이용하기 위해 SqlService 및 xml 파일을 아래와 같이 생성한다
```java
package springbook.user.sqlservice;

import springbook.user.dao.UserDao;
import springbook.user.sqlservice.jaxb.SqlType;
import springbook.user.sqlservice.jaxb.Sqlmap;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class XmlSqlService implements SqlService {
    private Map<String, String> sqlMap = new HashMap<String, String>();

    public XmlSqlService() {
        String contextPath = Sqlmap.class.getPackage().getName();
        try {
            JAXBContext context = JAXBContext.newInstance(contextPath);
            Unmarshaller unmarshaller = context.createUnmarshaller();
            InputStream is = UserDao.class.getResourceAsStream("/sqlmap.xml");
            Sqlmap sqlmap = (Sqlmap) unmarshaller.unmarshal(is);

            for (SqlType sql : sqlmap.getSql()) {
                sqlMap.put(sql.getKey(), sql.getValue());
            }
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getSql(String key) throws SqlRetrievalFailureException {
        String sql = sqlMap.get(key);
        if (sql == null)
            throw new SqlRetrievalFailureException(key + "를 이용해서 SQL을 찾을 수 없습니다.");
        else
            return sql;
    }
}

```

```xml
<?xml version="1.0" encoding="UTF-8"?>
<sqlmap xmlns="http://www.epril.com/sqlmap"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:schemaLocation="http://www.epril.com/sqlmap ../../../../../../sqlmap.xsd">
    <sql key="userAdd">insert into users(id, name, password, email, level, login, recommend) values (?,?,?,?,?,?,?)</sql>
    <sql key="userGet">select * from users where id = ?</sql>
    <sql key="userGetAll">select * from users order by id</sql>
    <sql key="userDeleteAll">delete from users</sql>
    <sql key="userGetCount">select count(*) from users</sql>
    <sql key="userUpdate">update users set name = ?, password = ?, email = ?, level = ?, login = ?, recommend = ?, where id = ?</sql>
</sqlmap>
```

#### 7.2.3 빈의 초기화 작업
* 생성자에서 예외가 발생할 수도 있는 복잡한 초기화 작업을 두는 것은 위험함, 별도의 초기 메서드를 구현한다
* 읽어들일 파일의 위치와 이름이 코드에 고정되어 있다는 점이 유동적이지 않다
* 해당 부분을 고치면 아래와 같다.
```java
public class XmlSqlService implements SqlService {
    private String sqlmapFile;
    ...

    public void setsqlmapFile(String sqlmapFile) {
        this.sqlmapFile = sqlmapFile;
    }

    public void loadSql() {
        String contextPath = Sqlmap.class.getPackage().getName();
        try {
            JAXBContext context = JAXBContext.newInstance(contextPath);
            Unmarshaller unmarshaller = context.createUnmarshaller();
            InputStream is = UserDao.class.getResourceAsStream("/sqlmap.xml");
            Sqlmap sqlmap = (Sqlmap) unmarshaller.unmarshal(is);

            for (SqlType sql : sqlmap.getSql()) {
                sqlMap.put(sql.getKey(), sql.getValue());
            }
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
    }
}
```

```java
...
@Value("${datasource.sqlfileName}")
String sqlfileName;
...
@Bean
public SqlService sqlService(){
        XmlSqlService sqlProvider = new XmlSqlService();
        sqlProvider.setsqlmapFile(sqlfileName);
        sqlProvider.loadSql();
        return sqlProvider;
        }
...
```

#### 7.2.4 변화를 위한 준비: 인터페이스 분리
* xml 이 아닌 다양한 파일 포맷에서도 작동하게 만들 수 있도록 인터페이스를 분리한다
* sql 을 읽는 sqlReader, sqlRegistry 인터페이스를 구현하면 아래와 같다.
```java
package springbook.user.sqlservice;

public interface SqlReader {
    void read(SqlRegistry sqlRegistry);
}

```
```java
package springbook.user.sqlservice;

public interface SqlRegistry {
    void registerSql(String key, String sql);

    String findSql(String key) throws SqlNotFoundException;
}

```

#### 7.2.5 자기참조 빈으로 시작하기
* XmlSqlService 에서 이제 SqlRegistry, SqlReader 인터페이스를 DI 받을 수 있도록 수정한다
* XmlSqlService 는 SqlRegistry, SqlReader 도 구현한 class 로 만든다(자기참조 빈) 이렇게하면
한 클래스가 마치 빈이 3개 등록된 것처럼 사용할 수 있다.
```java
package springbook.user.sqlservice;

import springbook.user.dao.UserDao;
import springbook.user.sqlservice.jaxb.SqlType;
import springbook.user.sqlservice.jaxb.Sqlmap;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class XmlSqlService implements SqlService, SqlRegistry, SqlReader {
    private Map<String, String> sqlMap = new HashMap<String, String>();
    private String sqlmapFile;

    private SqlReader sqlReader;
    private SqlRegistry sqlRegistry;

    public void setSqlReader(SqlReader sqlReader) {
        this.sqlReader = sqlReader;
    }

    public void setSqlRegistry(SqlRegistry sqlRegistry) {
        this.sqlRegistry = sqlRegistry;
    }

    public XmlSqlService() {
        String contextPath = Sqlmap.class.getPackage().getName();
    }

    public void setsqlmapFile(String sqlmapFile) {
        this.sqlmapFile = sqlmapFile;
    }

    public void loadSql() {
        this.sqlReader.read(this.sqlRegistry);
    }

    @Override
    public String getSql(String key) throws SqlRetrievalFailureException {
        try {
            return this.sqlRegistry.findSql(key);
        } catch (SqlNotFoundException e) {
            throw new SqlRetrievalFailureException(e);
        }
    }

    @Override
    public String findSql(String key) throws SqlNotFoundException {
        String sql = sqlMap.get(key);
        if (sql == null) throw new SqlNotFoundException(key + "에 대한 SQL 을 찾을 수 없습니다");
        else return sql;
    }

    @Override
    public void registerSql(String key, String sql) {
        sqlMap.put(key, sql);
    }

    @Override
    public void read(SqlRegistry sqlRegistry) {
        String contextPath = Sqlmap.class.getPackage().getName();
        try {
            JAXBContext context = JAXBContext.newInstance(contextPath);
            Unmarshaller unmarshaller = context.createUnmarshaller();
            InputStream is = UserDao.class.getResourceAsStream(sqlmapFile);
            Sqlmap sqlmap = (Sqlmap) unmarshaller.unmarshal(is);

            for (SqlType sql : sqlmap.getSql()) {
                sqlRegistry.registerSql(sql.getKey(), sql.getValue());
            }
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
    }
}

```

#### 7.2.6 디폴트 의존관계
* DI 된 SqlRegistry, SqlReader 를 이용하여 사용하는 기본적인 Service 를 생성해보면 아래와 같다.
```java
public class BaseSqlService implements SqlService {
    protected SqlReader sqlReader;
    protected SqlRegistry sqlRegistry;

    public void setSqlReader(SqlReader sqlReader) {
        this.sqlReader = sqlReader;
    }

    public void setSqlRegistry(SqlRegistry sqlRegistry) {
        this.sqlRegistry = sqlRegistry;
    }

    public void loadSql() {
        this.sqlReader.read(this.sqlRegistry);
    }


    @Override
    public String getSql(String key) throws SqlRetrievalFailureException {
        try {
            return this.sqlRegistry.findSql(key);
        } catch (SqlNotFoundException e) {
            throw new SqlRetrievalFailureException(e);
        }
    }
}
```
* XmlSqlService 에서 사용하던 SqlRegistry 및 SqlReader 를 분리해서 클래스화 하면 아래와 같다.
```java
public class HashMapSqlRegistry implements SqlRegistry {
    private Map<String, String> sqlMap = new HashMap<String, String>();

    public String findSql(String key) throws SqlNotFoundException {
        String sql = sqlMap.get(key);
        if (sql == null)
            throw new SqlNotFoundException(key + "를 이용해서 SQL을 찾을 수 없습니다");
        else return sql;
    }

    public void registerSql(String key, String sql) {
        sqlMap.put(key, sql);
    }
}

```
```java
public class JaxbXmlSqlReader implements SqlReader {
    private String sqlmapFile;

    public void setSqlmapFile(String sqlmapFile) {
        this.sqlmapFile = sqlmapFile;
    }

    @Override
    public void read(SqlRegistry sqlRegistry) {
        String contextPath = Sqlmap.class.getPackage().getName();
        try {
            JAXBContext context = JAXBContext.newInstance(contextPath);
            Unmarshaller unmarshaller = context.createUnmarshaller();
            InputStream is = UserDao.class.getResourceAsStream(sqlmapFile);
            Sqlmap sqlmap = (Sqlmap) unmarshaller.unmarshal(is);

            for (SqlType sql : sqlmap.getSql()) {
                sqlRegistry.registerSql(sql.getKey(), sql.getValue());
            }
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
    }
}
```
* 해당 클래스들을 이용해서 Bean 설정을 변경하면 아래와 같다.
```java
    @Bean
    public SqlService sqlService() {
        BaseSqlService sqlProvider = new BaseSqlService();
        JaxbXmlSqlReader sqlReader = new JaxbXmlSqlReader();
        sqlReader.setSqlmapFile(sqlfileName);
        HashMapSqlRegistry sqlRegistry = new HashMapSqlRegistry();
        sqlProvider.setSqlReader(sqlReader);
        sqlProvider.setSqlRegistry(sqlRegistry);
        sqlProvider.loadSql();
        return sqlProvider;
    }
```
* DI 를 통하지 않고 기본적으로 사용할 SqlReader, SqlRegistry 를 정의할 수도 있다.
* 생성자를 통해 정의하면 아래와 같다.
```java
public class DefaultSqlService extends BaseSqlService {
    public DefaultSqlService() {
        setSqlReader(new JaxbXmlSqlReader());
        setSqlRegistry(new HashMapSqlRegistry());
    }
}
```
* 해당 빈을 등록하고 test 를 실행해보면 에러가 난다, xml 파일 경로를 set 해주지 않았기 때문이다.
설정하지 않고 기본적으로 사용할 Class 를 정의했는데 설정파일을 통해 sql 파일명을 주입해주는 것은 의도에 맞지 않으므로
  Default sql 파일 이름을 아래와 같이 지정해준다.
 ```java
public class JaxbXmlSqlReader implements SqlReader {
    private static final String DEFAULT_SQLMAP_FILE = "/sqlmap.xml";

    private String sqlmapFile = DEFAULT_SQLMAP_FILE;

    public void setSqlmapFile(String sqlmapFile) {
        this.sqlmapFile = sqlmapFile;
    }
    ...
```
* DefaultSqlService 설정 후 테스트를 돌려보면 성공한다, 일련의 과정을 통해 설정파일 간소화 및 확장 구현이 가능한 설계가 되었다.

### 7.3 서비스 추상화 적용
#### 7.3.1 OXM 서비스 추상화
* XML <-> Object 상호 변환 기술을 OXM 이라고 한다.
* 스프링에서 사용하는 OXM 추상화 기술은 Marshaller, UnMashaller 인터페이스를 포함한다
* 학습 테스트를 만들어보면 아래와 같다.
```java
    @Bean
    public Unmarshaller unmarshaller(){
        Jaxb2Marshaller unMarshaller = new Jaxb2Marshaller();
        unMarshaller.setContextPath("springbook.user.sqlservice.jaxb");
        return unMarshaller;
    }
```
```java
@SpringBootTest
@ContextConfiguration(classes = {BeanFactory.class})
public class OxmTest {
    @Autowired
    Unmarshaller unmarshaller;

    @Test
    public void unmarshallSqlMap() throws XmlMappingException, IOException, JAXBException {
        Source xmlSource = new StreamSource(getClass().getResourceAsStream("/sqlmap.xml"));

        Sqlmap sqlmap = (Sqlmap) this.unmarshaller.unmarshal(xmlSource);

        List<SqlType> sqlList = sqlmap.getSql();

        assertThat(sqlList.size(), is(6));
        assertThat(sqlList.get(0).getKey(), is("userAdd"));
    }

}
```
* Unmarshaller 인터페이스를 통해 Jaxb 와의 결합이 추상화 되었다

#### 7.3.2 OXM 서비스 추상화 적용
* OxmSqlService 를 생성해서 적용할 수 있도록 한다, SqlReader 클래스를 OxmSqlService 의 내부 스태틱 클래스로
선언하여 응집도를 높인다.
```java
public class OxmSqlService implements SqlService {
    private final OxmSqlReader oxmSqlReader = new OxmSqlReader();

    private SqlRegistry sqlRegistry = new HashMapSqlRegistry();

    public void setSqlRegistry(SqlRegistry sqlRegistry) {
        this.sqlRegistry = sqlRegistry;
    }

    public void setUnmarShaller(Unmarshaller unmarshaller) {
        this.oxmSqlReader.setUnmarshaller(unmarshaller);
    }

    public void setSqlmapFile(String sqlmapFile) {
        this.oxmSqlReader.setSqlmapFile(sqlmapFile);
    }

    public void loadSql() {
        this.oxmSqlReader.read(this.sqlRegistry);
    }

    public String getSql(String key) throws SqlRetrievalFailureException {
        try {
            return this.sqlRegistry.findSql(key);
        } catch (SqlNotFoundException e) {
            throw new SqlRetrievalFailureException(e);
        }
    }

    private class OxmSqlReader implements SqlReader {
        private Unmarshaller unmarshaller;
        private final static String DEFAULT_SQLMAP_FILE = "/sqlmap.xml";
        private String sqlmapFile = DEFAULT_SQLMAP_FILE;

        public void setUnmarshaller(Unmarshaller unmarshaller) {
            this.unmarshaller = unmarshaller;
        }

        public void setSqlmapFile(String sqlmapFile) {
            this.sqlmapFile = sqlmapFile;
        }

        @Override
        public void read(SqlRegistry sqlRegistry) {
            try {
                Source source = new StreamSource(UserDao.class.getResourceAsStream(this.sqlmapFile));
                Sqlmap sqlmap = (Sqlmap) this.unmarshaller.unmarshal(source);

                for (SqlType sql : sqlmap.getSql()) {
                    sqlRegistry.registerSql(sql.getKey(), sql.getValue());
                }
            } catch (IOException e) {
                throw new IllegalArgumentException(this.sqlmapFile + "을 가져올 수 없습니다.");
            }
        }
    }
}

```
```java
   @Bean
    public SqlService sqlService() {
        OxmSqlService sqlProvider = new OxmSqlService();
        sqlProvider.setUnmarShaller(unmarshaller());
        sqlProvider.loadSql();
        return sqlProvider;
    }

    @Bean
    public Unmarshaller unmarshaller(){
        Jaxb2Marshaller unMarshaller = new Jaxb2Marshaller();
        unMarshaller.setContextPath("springbook.user.sqlservice.jaxb");
        return unMarshaller;
    }
```

* BaseSqlService 와 OxmSqlService 의 getSql() 매서드가 중복된다.
현재는 간단한 코드이지만 나중을 위해 중복 코드를 줄이는 방법은, OxmSqlService -> BaseSqlService 로 
  동작을 위힘하는 것이다. 위임하도록 수정하면 아래와 같다.
```java
   public void loadSql() {
        this.baseSqlService.setSqlReader(this.oxmSqlReader);
        this.baseSqlService.setSqlRegistry(this.sqlRegistry);

        this.baseSqlService.loadSql();
    }

    public String getSql(String key) throws SqlRetrievalFailureException {
        try {
            return this.baseSqlService.getSql(key);
        } catch (SqlNotFoundException e) {
            throw new SqlRetrievalFailureException(e);
        }
    }
```  

#### 7.3.3 리소스 추상화
* sql 파일을 읽어오는 부분은, 파일이 될 수도 있고, http 등 여러 가지가 존재한다
* Spring 에서는 이 부분을 추상화하기 위해서 Resource 클래스를 제공한다. 사용하도록 수정하면 아래와 같다.
```java
public class OxmSqlService implements SqlService {
    ...
    public void setSqlmap(Resource sqlmap) {
        this.oxmSqlReader.setSqlmap(sqlmap);
    }
    ...
    private class OxmSqlReader implements SqlReader {
        private Unmarshaller unmarshaller;
        private final static String DEFAULT_SQLMAP_FILE = "/sqlmap.xml";
        private Resource sqlmap = new ClassPathResource("/sqlmap.xml", UserDao.class);
        ...
        public void setSqlmap(Resource sqlmap) {
            this.sqlmap = sqlmap;
        }

        @Override
        public void read(SqlRegistry sqlRegistry) {
            try {
                Source source = new StreamSource(sqlmap.getInputStream());
                Sqlmap sqlmap = (Sqlmap) this.unmarshaller.unmarshal(source);
                ...
```
```java
@Bean
    public SqlService sqlService() {
        OxmSqlService sqlProvider = new OxmSqlService();
        sqlProvider.setUnmarShaller(unmarshaller());
        sqlProvider.setSqlmap(new ClassPathResource("/sqlmap.xml"));
        sqlProvider.loadSql();
        return sqlProvider;
    }
```

### 7.4 인터페이스 상속을 통한 안전한 기능확장
* 운영중 sql 이 실시간으로 reload 되어야 할 경우에 대해 구현해본다

#### 7.4.1 DI와 기능의 확장
* 스프링을 통해 DI 를 구현하는 것은 아주 쉬운 일이나, 프로그램 설계 시 DI 를 고려하여
설계하는 것은 많은 경험, 공부가 필요하다
* DI 가 필요한 이유 또 하나로는 클래스 상속과는 다르게 인터페이스를 통해 같은 클래스라도
클라이언트에 따라 여러가지 인터페이스를 제공해 줄 수 있기 떄문이다. 클래스를 통해 구현하면
이와 같은 제공은 불가능하다.
 
#### 7.4.2 인터페이스 상속
* 업데이트 가능한 기능을 제공하기 위해 기존 SqlRegistry 를 상속해서 인터페이스를 구현한다.
```java
public interface UpdatableSqlRegistry extends SqlRegistry {
    public void updateSql(String key, String sql) throws SqlUpdateFailureException;

    public void updateSql(Map<String,String> sqlmap) throws SqlUpdateFailureException;
}
```
* 해당 인터페이스는 업데이트가 필요한 Service 에서만 사용하도록 주입해 줄 수 있다.
* 아래와 같이 SqlAdminService 에 사용되도록 주입될 수 있다.
```java
public class SqlAdminService implements AdminEventListener{
    private UpdatableSqlRegistry updatableSqlRegistry;

    public void setUpdatableSqlRegistry(UpdatableSqlRegistry updatableSqlRegistry){
        this.updatableSqlRegistry = updatableSqlRegistry;
    }

    public void updateEventListener(UpdateEvent event){
        this.updatableSqlRegistry.updateSql(event.get(KEY_ID), event.get(EVENT_ID));
    }
}
```

### 7.5 DI를 이용해 다양한 구현 방법 적용하기
#### 7.5.1 ConcurrentHashMap을 이용한 수정 가능 SQL 레지스트리
* 멀티쓰레드 동기화 관련해서 좋은 성능을 제공하는 ConcurrentHashMap을 이용해서 sqlmap 을 제공해본다
* 테스트 부터 생성해보면 아래와 같다
```java
public class ConcurrentHashMapRegistryTest {
    UpdatableSqlRegistry sqlRegistry;

    @BeforeAll
    public void setUp() {
        sqlRegistry = new ConcurrentHashMapRegistry();
        sqlRegistry.registerSql("KEY1", "SQL1");
        sqlRegistry.registerSql("KEY2", "SQL2");
        sqlRegistry.registerSql("KEY3", "SQL3");
    }

    @Test
    public void find() {
        checkFindResult("SQL1", "SQL2", "SQL3");

    }

    private void checkFindResult(String expected1, String expected2, String expected3) {
        assertThat(sqlRegistry.findSql("KEY1"), is(expected1));
        assertThat(sqlRegistry.findSql("KEY2"), is(expected2));
        assertThat(sqlRegistry.findSql("KEY3"), is(expected3));
    }

    @Test
    public void unknownKey() {
        assertThrows(SqlNotFoundException.class, () -> {
            sqlRegistry.findSql("SQL9999!@#$");
        });
    }

    @Test
    public void updateSingle() {
        sqlRegistry.updateSql("KEY2", "Modified2");
        checkFindResult("SQL1", "Modified2", "SQL3");
    }

    @Test
    public void updateMulti() {
        Map<String, String> sqlmap = new HashMap<>();
        sqlmap.put("KEY1", "Modified1");
        sqlmap.put("KEY3", "Modified3");

        sqlRegistry.updateSql(sqlmap);
        checkFindResult("Modified1", "SQL2", "Modified3");
    }

    @Test
    public void updateWithNotExistingKey() {
        assertThrows(SqlUpdateFailureException.class, () -> {
            sqlRegistry.updateSql("SQL9999!@#$", "Modified2");
        });
    }
}

```
* ConcurrentHashMapRegistry 구현하면 아래와 같다.
```java
public class ConcurrentHashMapRegistry implements UpdatableSqlRegistry {
    ConcurrentHashMap<String, String> sqlMap = new ConcurrentHashMap<>();

    @Override
    public String findSql(String key) throws SqlNotFoundException {
        String sql = sqlMap.get(key);
        if (sql == null) {
            throw new SqlNotFoundException(key + " 를 이용해서 SQL 을 찾을 수 없습니다.");
        } else return sql;
    }

    @Override
    public void registerSql(String key, String sql) {
        sqlMap.put(key, sql);
    }

    @Override
    public void updateSql(String key, String sql) throws SqlUpdateFailureException {
        if (sqlMap.get(key) == null) {
            throw new SqlUpdateFailureException(key + " 에 해당하는 SQL을 찾을 수 없습니다.");
        }

        sqlMap.put(key, sql);
    }

    @Override
    public void updateSql(Map<String, String> sqlmap) throws SqlUpdateFailureException {
        for (Map.Entry<String, String> entry : sqlmap.entrySet()) {
            updateSql(entry.getKey(), entry.getValue());
        }
    }
}
```

#### 7.5.2 내장형 데이터베이스를 이용한 SQL 레지스트리 만들기
* SqlRegistry 를 구현하기 위해 내장형 DB 를 사용할 수 도 있다.
* 스프링에서 제공하는 내장형 DB 를 테스트 해보기 위해 우선 초기 sql 파일을 만든다.
```sql
CREATE TABLE SQLMAP(
    KEY_ VARCHAR(100) PRIMARY KEY,
    SQL_ VARCHAR(100) NOT NULL
);
```
```sql
INSERT INTO SQLMAP(KEY_, SQL_) values ('KEY1', 'SQL1');
INSERT INTO SQLMAP(KEY_, SQL_) values ('KEY2', 'SQL2');
```
* 초기 SQL 파일은 구성되었으니 스프링에서 제공하는 EmbeddedDatabaseBuilder 를 이용한 내장형 DB
사용 학습 테스트를 만들면 아래와 같다.
```java
public class EmbeddedDbTest {
  static EmbeddedDatabase db;
  static JdbcTemplate template;

  @BeforeAll
  public static void setUp() {
    db = new EmbeddedDatabaseBuilder()
            .setType(H2)
            .addScript("/schema.sql")
            .addScript("/data.sql")
            .build();
    template = new JdbcTemplate(db);
  }

  @AfterAll
  public static void tearDown() {
    db.shutdown();
  }

  @Test
  public void initData() {
    assertThat(template.queryForObject("select count(*) from sqlmap", Integer.class), is(2));

    List<Map<String, Object>> list = template.queryForList("select * from sqlmap order by key_");
    assertThat((String) list.get(0).get("key_"), is("KEY1"));
    assertThat((String) list.get(0).get("sql_"), is("SQL1"));
    assertThat((String) list.get(1).get("key_"), is("KEY2"));
    assertThat((String) list.get(1).get("sql_"), is("SQL2"));
  }
    
  @Test
  public void insert() {
    template.update("insert into sqlmap(key_, sql_) values(?,?)", "KEY3", "SQL3");

    assertThat(template.queryForObject("select count(*) from sqlmap", Integer.class), is(3));
  }
}
```  
* 실제 SqlResigtry 에 적용하면 아래와 같다.
```java
public class EmbeddedDbSqlRegistry implements UpdatableSqlRegistry {
    JdbcTemplate jdbc;

    public void setDataSource(DataSource dataSource) {
        jdbc = new JdbcTemplate(dataSource);
    }

    @Override
    public void registerSql(String key, String sql) {
        jdbc.update("insert into sqlmap(key_, sql_) values(?,?)", key, sql);
    }

    @Override
    public String findSql(String key) throws SqlNotFoundException {
        try {
            return this.jdbc.queryForObject("select sql_ from sqlmap where key_ = ?", String.class, key);
        } catch (EmptyResultDataAccessException e) {
            throw new SqlNotFoundException(key + "에 해당하는 SQL을 찾을 수 없습니다.", e);

        }
    }

    @Override
    public void updateSql(String key, String sql) throws SqlUpdateFailureException {
        int affected = jdbc.update("update sqlmap set sql_ = ? where key_ = ?", sql, key);
        if (affected == 0) {
            throw new SqlUpdateFailureException(key + "에 해당하는 SQL을 찾을 수 없습니다.");
        }
    }

    @Override
    public void updateSql(Map<String, String> sqlmap) throws SqlUpdateFailureException {
        for (Map.Entry<String, String> entry : sqlmap.entrySet()) {
            updateSql(entry.getKey(), entry.getValue());
        }
    }
}

```
* EmbeddedDbSqlRegistry 에 대한 테스트는 기존의 ConcurrentHashMapSqlRegistry 와 인터페이스가
겹치므로, 테스트 코드를 아래와 같이 상속하여 사용한다.
```java
public abstract class AbstractUpdatableSqlRegistryTest {
    UpdatableSqlRegistry sqlRegistry;


    @BeforeEach
    public void setUp(){
        sqlRegistry = createUpdatableSqlRegistry();
        sqlRegistry.registerSql("KEY1", "SQL1");
        sqlRegistry.registerSql("KEY2", "SQL2");
        sqlRegistry.registerSql("KEY3", "SQL3");
    }

    abstract protected UpdatableSqlRegistry createUpdatableSqlRegistry();

    @Test
    public void find() {
        checkFindResult("SQL1", "SQL2", "SQL3");

    }

    protected void checkFindResult(String expected1, String expected2, String expected3) {
        assertThat(sqlRegistry.findSql("KEY1"), is(expected1));
        assertThat(sqlRegistry.findSql("KEY2"), is(expected2));
        assertThat(sqlRegistry.findSql("KEY3"), is(expected3));
    }

    @Test
    public void unknownKey() {
        assertThrows(SqlNotFoundException.class, () -> {
            sqlRegistry.findSql("SQL9999!@#$");
        });
    }

    @Test
    public void updateSingle() {
        sqlRegistry.updateSql("KEY2", "Modified2");
        checkFindResult("SQL1", "Modified2", "SQL3");
    }

    @Test
    public void updateMulti() {
        Map<String, String> sqlmap = new HashMap<>();
        sqlmap.put("KEY1", "Modified1");
        sqlmap.put("KEY3", "Modified3");

        sqlRegistry.updateSql(sqlmap);
        checkFindResult("Modified1", "SQL2", "Modified3");
    }

    @Test
    public void updateWithNotExistingKey() {
        assertThrows(SqlUpdateFailureException.class, () -> {
            sqlRegistry.updateSql("SQL9999!@#$", "Modified2");
        });
    }
}
```
```java
public class EmbeddedDbSqlRegistryTest extends AbstractUpdatableSqlRegistryTest {
    EmbeddedDatabase db;
    EmbeddedDbSqlRegistry embeddedDbSqlRegistry;

    @Override
    protected UpdatableSqlRegistry createUpdatableSqlRegistry() {
        db = new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.HSQL)
                .addScript("/schema.sql")
                .build();

        embeddedDbSqlRegistry = new EmbeddedDbSqlRegistry();
        embeddedDbSqlRegistry.setDataSource(db);
        return embeddedDbSqlRegistry;
    }

    @AfterEach
    public void tearDown() {
        db.shutdown();
    }
}
```

#### 7.5.3 트랜잭션 적용
* 현재 내장형 DB를 이용해서 sql 수정은 정상적으로 잘 되나, 트랜잭션 처리가 되어있지 않다
* 트랜잭션 적용을 위해 에러 상황을 test 메서드로 추가한다
```java
public class EmbeddedDbSqlRegistryTest extends AbstractUpdatableSqlRegistryTest {
    ...
    @AfterEach
    public void tearDown() {
        db.shutdown();
    }

    @Test
    public void transactionalUpdate(){
        checkFindResult("SQL1", "SQL2", "SQL3");

        Map<String, String> sqlmap = new HashMap<String,String>();
        sqlmap.put("KEY1", "Modified1");
        sqlmap.put("KEY9999!@#$", "Modified9999");

        try{
            sqlRegistry.updateSql(sqlmap);
            fail();
        }catch(SqlUpdateFailureException e){}

        checkFindResult("SQL1", "SQL2", "SQL3");

    }

}
```
* 트랜잭션 적용을 위해 간단한 TransactionTemplate 을 적용한다, 이 template 은 Bean 으로 따로 등록하지 않고
내장 DB Registry 에 내부적으로 생성하여 적용한다.
```java
public class EmbeddedDbSqlRegistry implements UpdatableSqlRegistry {
    ...
    @Override
    public void updateSql(final Map<String, String> sqlmap) throws SqlUpdateFailureException {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                for (Map.Entry<String, String> entry : sqlmap.entrySet()) {
                    updateSql(entry.getKey(), entry.getValue());
                }
            }
        });
    }
}
```

### 7.6 스프링 3.1의 DI
* 스프링의 변화에 대비한 설계 때문에, 1.0 과 3.1 은 거의 완벼한 호환성을 자랑한다
* 자바가 그동안 업데이트 되면서 스프링의 사용방식에도 많은 변화가 있었다. 대표적인 변화는 아래 두개와 같다
  1. 애노테이션의 메타정보 활용 
      * 애노테이션을 통해 기존 XML 로 제공되던 스프링의 설정을 간략화 할수 있고 소스에서 타입 확인등을 할 수 있게 되었다.
      * 하지만 XML 과는 달리 수정 시 컴파일을 다시해야 한다는 수고스러움이 있다.
      * 현재는 애노테이션을 통한 설정으로 대세가 바뀌어 가고 있다.
  2. 정책과 관례를 이용한 프로그래밍
    * 정책과 관례를 이용한 프로그래밍이라는 것은, 스프링 XML 설정의 <bean> 태그를 통해 객체가 생성되듯이 미리 약속된 규약을 통해 프로그래밍 한다는 것이다.
    * 소스코드 간결화 등 많은 이점을 주지만, 정책과 관례를 이해하는데 시간이 걸리며 잘못 사용시 이해할 수 없는 버그를 만들어내기도 한다.
    * 예제를 따라해보면서 애노테이션이 어떤 이점을 주는지 확인해본다


* 해당 장은, 애초부터 Bean 을 통하여 테스트 코드를 작성하였으므로 넘어가도록 함