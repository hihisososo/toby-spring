package springbook.user.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User {
    String id;
    String name;
    String password;
    Level level;
    int login;
    int recommend;
    String eamil;

    public void upgradeLevel(){
        Level nextLevel = level.nextLevel();
        if(nextLevel == null){
            throw new IllegalStateException(this.level + "은 업그레이드가 불가능합니다");
        }else{
            this.level = nextLevel;
        }
    }
}
