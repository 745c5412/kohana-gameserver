package koh.game.dao;

import com.google.inject.Binder;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import koh.game.app.Loggers;
import koh.game.app.MemoryService;
import koh.game.dao.api.*;
import koh.game.dao.mysql.*;
import koh.game.dao.script.ArenaBattleDAOImpl;
import koh.game.dao.script.ChallengeDAOImpl;
import koh.game.dao.script.MonsterMindDAOImpl;
import koh.game.dao.script.PlayerCommandDAOImpl;
import koh.game.dao.sqlite.GuildDAOImpl;
import koh.game.dao.sqlite.GuildMemberDAOImpl;
import koh.game.dao.sqlite.MountInventoryDAOImpl;
import koh.game.dao.sqlite.PetInventoryDAOImpl;
import koh.game.entities.guilds.GuildMember;
import koh.game.utils.Settings;
import koh.game.utils.sql.ConnectionResult;
import koh.game.utils.sql.ConnectionStatement;
import koh.patterns.services.api.DependsOn;
import koh.patterns.services.api.Service;
import lombok.extern.log4j.Log4j2;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * @author Neo-Craft
 */
@Log4j2
@DependsOn({Loggers.class, MemoryService.class})
public class DatabaseSource implements Service {

    @Override
    public void inject(Injector injector) {
    }

    @Override
    public void configure(Binder binder) {
        binder.bind(AccountDataDAO.class).to(AccountDataDAOImpl.class).asEagerSingleton();
        binder.bind(AreaDAO.class).to(AreaDAOImpl.class).asEagerSingleton();
        binder.bind(D2oDAO.class).to(D2oDAOImpl.class).asEagerSingleton();
        binder.bind(ExpDAO.class).to(ExpDAOImpl.class).asEagerSingleton();
        binder.bind(GuildDAO.class).to(GuildDAOImpl.class).asEagerSingleton();
        binder.bind(GuildEmblemDAO.class).to(GuildEmblemDAOImpl.class).asEagerSingleton();
        binder.bind(GuildMemberDAO.class).to(GuildMemberDAOImpl.class).asEagerSingleton();
        binder.bind(ItemDAO.class).to(ItemDAOImpl.class).asEagerSingleton();
        binder.bind(ItemTemplateDAO.class).to(ItemTemplateDAOImpl.class).asEagerSingleton();
        binder.bind(JobDAO.class).to(JobDAOImpl.class).asEagerSingleton();
        binder.bind(MapDAO.class).to(MapDAOImpl.class).asEagerSingleton();
        binder.bind(MonsterDAO.class).to(MonsterDAOImpl.class).asEagerSingleton();
        binder.bind(MountDAO.class).to(MountDAOImpl.class).asEagerSingleton();
        binder.bind(MountInventoryDAO.class).to(MountInventoryDAOImpl.class).asEagerSingleton();
        binder.bind(NpcDAO.class).to(NpcDAOImpl.class).asEagerSingleton();
        binder.bind(PaddockDAO.class).to(PaddockDAOImpl.class).asEagerSingleton();
        binder.bind(PetInventoryDAO.class).to(PetInventoryDAOImpl.class).asEagerSingleton();
        binder.bind(PlayerCommandDAO.class).to(PlayerCommandDAOImpl.class).asEagerSingleton();
        binder.bind(PlayerDAO.class).to(PlayerDAOImpl.class).asEagerSingleton();
        binder.bind(SpellDAO.class).to(SpellDAOImpl.class).asEagerSingleton();
        binder.bind(MapMonsterDAO.class).to(MapMonsterDAOImpl.class).asEagerSingleton();
        binder.bind(PlayerCommandDAO.class).to(PlayerCommandDAOImpl.class).asEagerSingleton();
        binder.bind(MonsterMindDAO.class).to(MonsterMindDAOImpl.class).asEagerSingleton();
        binder.bind(ArenaBattleDAO.class).to(ArenaBattleDAOImpl.class).asEagerSingleton();
        binder.bind(PresetDAO.class).to(PresetDAOImpl.class).asEagerSingleton();
        binder.bind(ChallengeDAO.class).to(ChallengeDAOImpl.class).asEagerSingleton();
        binder.bind(TaxCollectorDAO.class).to(TaxCollectorDAOImpl.class).asEagerSingleton();
        binder.bind(AchievementDAO.class).to(AchievementDAOImpl.class).asEagerSingleton();
        binder.requestStaticInjection(DAO.class);
    }

    @Inject
    private Settings settings;

    private static HikariDataSource dataSource;

    public void setNewConnection() {
        final HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://" + settings.getStringElement("Database.Host") + "/" + settings.getStringElement("Database.Name"));
        config.setUsername(settings.getStringElement("Database.User"));
        config.setPassword(settings.getStringElement("Database.Password"));
        /*config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("leakDetectionThreshold","38000");
        config.addDataSourceProperty("validationTimeout","7000");
        config.addDataSourceProperty("maxLifetime","300000");
        config.setConnectionTestQuery("SELECT current_timestamp");*/
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("leakDetectionThreshold", "20000");
        config.addDataSourceProperty("validationTimeout", "5000");
        config.setMaximumPoolSize(50);

        this.dataSource = new HikariDataSource(config);
    }

    @Override
    public void start() {
        if (dataSource != null && !dataSource.isClosed())
            dataSource.close();

        this.setNewConnection();

        final Field[] fields = DAO.class.getDeclaredFields();
        for (Field field : fields) {
            if (field.getType() == DatabaseSource.class) {
                continue;
            }
            if (Modifier.isStatic(field.getModifiers()) && Service.class.isAssignableFrom(field.getType())) {
                try {
                    field.setAccessible(true);
                    ((Service) field.get(null)).start();
                } catch (Exception e) {
                    log.error(e);
                    log.warn(e.getMessage());
                }
            }
        }
    }


    @Override
    public void stop() {
        System.out.println("Player saving....");
        DAO.getPlayers().getPlayers().forEach(pl -> pl.save(false));
        System.out.println("Guild saving....");
        DAO.getGuilds().asStream().forEach(Guild ->
                Guild.memberStream().forEach(GuildMember::save)
        );
        System.out.println("GuildMember saving....");
        DAO.getGuilds().getEntites().forEach(DAO.getGuilds()::update);

        Field[] fields = DAO.class.getDeclaredFields();
        for (Field field : fields) {
            if (field.getType() == DatabaseSource.class) {
                continue;
            }
            if (Modifier.isStatic(field.getModifiers()) && Service.class.isAssignableFrom(field.getType())) {
                try {
                    field.setAccessible(true);
                    ((Service) field.get(null)).stop();
                } catch (Exception e) {
                    log.error(e);
                    log.warn(e.getMessage());
                }
            }
        }

        if (dataSource != null)
            dataSource.close();

    }

    public Connection getConnectionOfPool() throws SQLException {
        try {
            return dataSource.getConnection();
        } catch (java.sql.SQLException e) {
            e.printStackTrace();
            log.error("Connection restarted due to the pool closed");
            this.forcedRestart();
            return dataSource.getConnection();
        }
    }

    public void forcedRestart() {
        final HikariDataSource source = this.dataSource;
        this.setNewConnection();
        try {
            source.close();
        } catch (Exception ee) {
            ee.printStackTrace();
        }
    }

    public ConnectionStatement<Statement> createStatement() throws SQLException {
        Connection connection = this.getConnectionOfPool();
        return new ConnectionStatement<>(connection, connection.createStatement());
    }

    public ConnectionStatement<PreparedStatement> prepareStatement(String query) throws SQLException {
        Connection connection = this.getConnectionOfPool();
        return new ConnectionStatement<>(connection, connection.prepareStatement(query));
    }

    public ConnectionStatement<PreparedStatement> prepareStatement(String query, boolean autoGeneratedKeys) throws SQLException {
        Connection connection = this.getConnectionOfPool();
        PreparedStatement statement = connection.prepareStatement(query,
                autoGeneratedKeys ? Statement.RETURN_GENERATED_KEYS : Statement.NO_GENERATED_KEYS);
        return new ConnectionStatement<>(connection, statement);
    }

    public ConnectionResult executeQuery(String query) throws SQLException {
        Connection connection = this.getConnectionOfPool();
        Statement statement = connection.createStatement();
        statement.setQueryTimeout(300);
        return new ConnectionResult(connection, statement, statement.executeQuery(query));
    }

    public ConnectionResult executeQuery(String query, int secsTimeout) throws SQLException {
        Connection connection = this.getConnectionOfPool();
        Statement statement = connection.createStatement();
        if (secsTimeout > 0)
            statement.setQueryTimeout(secsTimeout);
        return new ConnectionResult(connection, statement, statement.executeQuery(query));
    }


}
