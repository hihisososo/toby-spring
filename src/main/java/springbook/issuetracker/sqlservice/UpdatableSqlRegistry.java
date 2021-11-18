package springbook.issuetracker.sqlservice;

import springbook.user.sqlservice.SqlRegistry;
import springbook.user.sqlservice.SqlUpdateFailureException;

import java.util.Map;

public interface UpdatableSqlRegistry extends SqlRegistry {
    public void updateSql(String key, String sql) throws SqlUpdateFailureException;

    public void updateSql(Map<String,String> sqlmap) throws SqlUpdateFailureException;
}
