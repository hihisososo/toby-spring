## 4장(예외)
### 4.1 사라진 SQLException
* UserDao 에서, JdbcTemplate 를 적용한 후로 SQLException 이 사라졌는데 이 Exception 은 어디로 간 것일까?
#### 4.1.1 초난감 예외처리
  
* 우선적으로 종종 발견되는 초난감 예외처리 코드이다
```java
try{
    ...
}
catch(SQLException e){
}
```
* 예외를 잡고는 아무것도 하지 않는 것은 무책임한 일이다.
* 아래와 같은 코드도 마찬가지다.

```java
catch(SQLException e){
    System.out.println(e);
}
```
```java
catch(SQLException e){
    e.printStackTrace();
}
```
* 모든 예외는 적절하게 복구되거나, 작업을 중단시키고 운영자 또는 개발자에게 분명하게 통보돼야 한다.
* 조치할 방법이 없다면 throw 하여 호출한 쪽으로 던져준다.
* throw 를 무책임하게 Exception 으로 공통지어 처리하는 코드를 사용하는 코드는 좋지 않다
```java
public void method1() throws Exception{
    method2();
    ...
}
public void method2() throws Exception{
    method3();
    ...
}
public void method3() throws Exception{
    ...
}
```
* throw Exception 의 문제는 복구할 수 있는 문제도 복구할 수 없도록 뭉뚱그려져서 처리된다는 점이다.
#### 4.1.2 예외의 종류와 특징
  
* 자바에서 throws 를 통해 발생시킬 수 있는 예외
    1. Error : java.lang.Error 의 서브클래스들(OOME, ThreadDeath)
    2. Exception 과 체크 예외 : java.lang.Exception 클래스 및 그 서브클래스
    Exception 클래스는 체크예외, 언체크 예외(RuntimeException 상속)로 구분됨
       
* 체크 예외에 대한 불필요함이 대두되면서 최근에 등장하는 API 들은 예상 가능한 예외상황을
다루는 예외를 체크 예외로 만들지 않는 경향이 있다.
  
#### 4.1.3 예외처리 방법
* 예외처리의 일반적인 방법은 아래와 같다.
    1. 예외 복구 : ex) 네트워크 불안 해서 에러 발생 시, 재시도
    2. 예외처리 회의 : 호출한 쪽으로 throw 시켜버림(무책임하게는 no)
    3. 예외 전환 : 발생한 예외를 감싸서 적절한 예외로 던짐(의미를 분명히 해줄 수 있음)
    
* 일반적으로 체크 예외를 계속 throw 하여 넘기는 것은 의미가 없음, SQLException 이 웹 컨트롤러 까지 간다고 해서 
무슨 소용이 있을까? 어차피 복구 불가능하면 위쪽에서 불필요한 throw 가 없도록 런타임 예외로 처리해야 한다.
  
#### 4.1.4 예외처리 전략
* 일반적으로 체크 예외는 일말이라도 복구 가능성이 있다면 명시적으로 catch 하여 복구하도록 하는 예외이다.
* 하지만 실제로는 너무 많아서 예외를 제대로 다루고 싶지 않을 만큼 짜증나게 하기도 한다.
* 자바가 서버 환경으로 오면서, 각 작업이 독립적으로 되며, 해당 작업이 문제가 있으면 그 문제발생 쓰레드만 취소시키면 그만인 환경이 만들어졌다.
  차라리 일반적으로 언체크 예외로 처리하고 개발자나 관리자에게 알려주는 편이 나아지게 되면서, 요즘에는 일반적으로 API 의 예외는 항상 복구가능하지 않으면
  언체크 예외로 만드는 경향이 있다.
  
* 사용자 추가 시 중복되는 키가 있으면 예외 전환하고, 그 외의 SQLException 이면 예외를 포장하는 코드를 짜보면 아래와 같다.
```java
public class DuplicateUserIdException extends RuntimeException {
    public DuplicateionUserIdException(Throwable cause) {
        super(cause);
    }
}
```
```java
public void add() throws DuplicateUserIdException{
        try{
            // JDBC 이용한 user 추가 코드
        }catch(SQLException e){
            if(e.getErrorCode() == MysqlErrornumbers.ER_DUP_ENTRY)
                throw new DuplicateUserIdException(e);  // 예외 전환
            else
                throw new RuntimeException(e);  // 예외 포장
        }
    }
```
* 런타임 예외를 일반화 해서 사용하는 방법은 장점이 많으나, 혹 처리가능한 예외를 놓칠 수 있다.
* API 문서를 통해 발생하는 예외의 종류와 원인, 활용 방법을 자세히 확인한 후 처리하여야 한다.

#### 4.1.5 SQLException 은 어떻게 됐나?
* 지금까지 다룬 내용은 JdbcTemplate 에서 왜 SQLException 이 사라졌나에 대한 설명에 필요한 것이다.
* SQLException 은 대부분 복구할 방법이 코드상에서는 없음
* 관리자나 개발자에게 빨리 알려지도록 조치하는 방법밖에는 없다.
* 따라서, 예외 전환을 통해 언체크/런타임 에러로 전환된 것이다.
* 스프링의 JdbcTemplate 은 모든 SQLException 을 런타임 예외인 DataAccessException 으로 포장해서 던져준다.

### 4.2 예외 전환
#### 4.2.1 JDBC 의 한계
* JDBC 는 추상화를 통해 표준 메소드만 익히면 여러 DB 를 동일하게 사용할 수 있게 해준다.
* 하지만, 비표준 SQL 과 SQLException 의 호환성없는 DB 에러정보(DB 마다의 에러코드 및 상태정보를 참조해야 함) 때문에 
DB 종류가 바뀌어도 DAO 를 수정하지 않으려면 힘든 부분이 있다.
  
#### 4.2.2 DB 에러 코드 매핑을 통한 전환
* SQLException 에 담긴 상태 코드는 신뢰할 수 없으므로 고려하지 않는다.
* 중구난방의 SQL 에러 코드를 어떻게 처리해야 할까?
* 방법 중 하나는 각 DB 별 에러 코드를 참고하여 매핑시켜 주는 것이다.
* 코드 내에서 하기는 복잡하므로, 스프링이 정의한 예외 클래스와 매핑해놓은 에러 코드 매핑정보 테이블을 통해 이를 이용한다.
* JdbcTemplate 안에서 발생하는 DataAccessException 안에는 여러 이유별로 세분화된 에러가 들어 있다. 해당 Exception 의 
서브 클래스를 통해 상세한 에러 정보를 알 수 있다.
  
#### 4.2.3 DAO 인터페이스와 DataAccessException 계층구조
* DAO 를 다로 사용하는 이유는 무엇일까?
* 데이터 액세스 코드를 다른 로직에서 분리해놓기 위함
* DAO 는 내부에서 어떤 데이터 액세스 기술을 사용하는지 신경쓰지 않아야 한다.
* 근데, 데이터 액세스 기술마다 throw 하는 Exception 이 다르다면 기술에 종속될 수 있다.
* 그렇다고 throw Exception 으로 처리하기엔 너무 무책임하다.
* JDO, Hibernate, JPA 기술은 RuntimeException 이므로 JDBC 의 SQLExcption 만 
런타임 예외로 포장해주면 throw 구문을 없앨 수 있다.
  
* throw 구문을 없앴으나, throw 되는 일부분의 복구가능한 예외처리는 추가해야 한다.
* 일부분의 복구 가능한 에러는 근데 JDO, Hibernate, JPA 마다 다르다.
* 그래서 스프링에서는 DataAccessException 을 계층화한 Exception 을 통해 
Data Access 기술에 따라 공통적으로 처리해준다.
  
#### 4.2.4 기술에 독립적인 UserDao 만들기
* UserDao 를 인터페이스와 구현체로 분리하는 과정이다.
* 인터페이스로 일단 분리하면 아래와 같다.
```java
public interface UserDao {
    void add(User user);
    User get(String id);
    List<User> getAll();
    void deleteAll();
    int getCount();
}
```
* 설정파일도 아래와 같이 바꾸어 준다
```java
public class DaoFactory {
    @Bean
    public UserDao userDao() {
        UserDaoJdbc userDao = new UserDaoJdbc();
        userDao.setDataSource(dataSource());
        return userDao;
    }
    ...
}
```
* 기존 UserDao 는 UserDaoJdbc 로 바꾼 후 UserDao 인터페이스를 구현하게 한다.

### 4.3 정리
* 마구잡이의 throws 선언은 위험하다, 복구가능한 에러라면 적절히 처리되어야 하고 아니라면, RuntimeException 으로 변경해야 한다.
* 애플리케이션의 중료 로직은 담기 위한 예외는 체크 예외로 만든다.
* JDBC의 SQLException 은 대부분 복구할 수 없는 예외이므로 헌타임 예외로 포장해야 한다.
* 스프링은 데이터 액세스 기술에 독립적으로 사용할 수 있도록 DataAccessException 이라는 추상화된 런타임 예외 계층을 제공한다.
* DAO 를 데이터 액세스 기술에서 독립시키려면 인터페이스화, 런타임전환, 기술에 독립적인 예외로 전환이 필요하다.