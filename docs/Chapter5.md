<h2>5장(서비스 추상화)</h2>
<h3>5.1 사용자 레벨 관리 기능 추가</h3>
* 지금까지 만들었던 DAO 는 CRUD 같은 기초적인 기능만 가능하다.
여기에 사용자 레벨 관리 기능을 추가해보자.
  
* 기능 상세는 아래와 같다.
    1. 사용자의 레벨은 BASIC, SILVER, GOLD 세 가지 중 하나다.
    2. 사용자가 처름 가입하면 BASIC 레벨이 되며, 이후 활동에 따라서 한 단계씩 업그레이드 될 수 있다.
    3. 가입 후 50회 이상 로그인을 하면 BASIC에서 SILVER 레벨이 된다.
    4. SILVER 레벨이면서 30번 이상 추천을 받으면 GOLD 레벨이 된다.
    5. 사용자 레벨의 변경 작업은 일정한 주기를 가지고 일괄적으로 진행된다,.
    변경 작업 전에는 조건을 충족하더라도 레벨의 변경이 일어나지 않는다.
       
<h4>5.1.1 필드 추가</h4>
* User 클래스에 사용자의 레벨을 저장할 필드를 추가하면 아래와 같다.
```java
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private static final int BASIC = 1;
    private static final int SILVER = 1;
    private static final int GOLD = 1;
    
    int level;
    String id;
    String name;
    String password;
}
```

* 이렇게 선언했을 때의 문제는, int 값이기 때문에 다른 int 정보를 넣는
  user.setLevel(other.getSum()) 과 같은 실수를 해도 컴파일러가 체크해주지 못한다는 점이다.
  
* 숫자 타입을 직접 사용하는 것보다는 enum 을 이용하는 것이 안전하고 편하다.
```java
package springbook.user.domain;

public enum Level {
    BASIC(1), SILVER(2), GOLD(3);

    private final int value;

    Level(int value) {
        this.value = value;
    }

    public int intValue() {
        return value;
    }

    public static Level valueOf(int value) {
        switch (value) {
            case 1:
                return BASIC;
            case 2:
                return SILVER;
            case 3:
                return GOLD;
            default:
                throw new AssertionError("Unknown value: " + value);
        }
    }
}
```
* 이렇게 만든 Level 타입의 변수를 User 클래스에 추가하고, 로그인 횟수와 추천수도 추가하면 아래와 같다.
```java
public class User {
    ...
    Level level;
    int login;
    int recommend;
}
```
* DB 테이블에도 Level, login, recommend 필드를 추가한다.
* UserDaoTest 도 수정하면 아래와 같다.
```java
public class UserDaoTest {
    ...
    @BeforeEach
    public void setUp() {
        this.user1 = new User("gyumee", "박성철", "springno1", Level.BASIC, 1, 0);
        this.user2 = new User("leegw700", "이길원", "springno2", Level.SILVER, 55, 10);
        this.user3 = new User("bumjin", "박범진", "springno3", Level.GOLD, 100, 40);
    }
```