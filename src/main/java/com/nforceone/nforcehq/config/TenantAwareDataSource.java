package com.nforceone.nforcehq.config;

import com.nforceone.nforcehq.security.TenantContext;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.springframework.jdbc.datasource.DelegatingDataSource;

/**
 * Wraps the real (pooled) DataSource so that every borrowed connection has the
 * app.tenant_id / app.user_id Postgres session variables set to whatever
 * TenantContext currently holds for this thread — these back the Row-Level Security
 * policies defined in V1__init_schema.sql. Session-level (not "SET LOCAL"/transaction
 * scoped) on purpose: Spring/Hikari hands out a connection fresh from the pool at the
 * start of each transaction before autocommit is turned off, so a transaction-scoped
 * SET LOCAL issued at checkout time would already have expired by the time the
 * transaction's real queries run. Setting it at session scope on every checkout is
 * safe here because every checkout corresponds to a new transaction for this app.
 */
public class TenantAwareDataSource extends DelegatingDataSource {

    private static final String APPLY_TENANT_SQL =
            "select set_config('app.tenant_id', ?, false), set_config('app.user_id', ?, false)";

    public TenantAwareDataSource(DataSource targetDataSource) {
        super(targetDataSource);
    }

    @Override
    public Connection getConnection() throws SQLException {
        Connection connection = super.getConnection();
        applyTenant(connection);
        return connection;
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        Connection connection = super.getConnection(username, password);
        applyTenant(connection);
        return connection;
    }

    private void applyTenant(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(APPLY_TENANT_SQL)) {
            statement.setString(1, TenantContext.getTenantIdOrEmpty());
            statement.setString(2, TenantContext.getUserIdOrEmpty());
            statement.execute();
        }
    }
}
