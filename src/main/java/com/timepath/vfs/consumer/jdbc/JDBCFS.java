package com.timepath.vfs.consumer.jdbc;

import com.timepath.vfs.MockFile;
import com.timepath.vfs.SimpleVFile;
import com.timepath.vfs.VFSStub;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.*;
import java.util.Collection;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author TimePath
 */
public abstract class JDBCFS extends VFSStub {

    private static final Logger LOG = Logger.getLogger(JDBCFS.class.getName());
    @NotNull
    protected final String url;
    protected final Connection conn;

    public JDBCFS(@NotNull String url) throws SQLException {
        this.url = url;
        this.name = url.replace('/', '\\');
        conn = DriverManager.getConnection(url);
    }

    @Nullable
    @Override
    public SimpleVFile get(@NotNull final String name) {
        for (@NotNull SimpleVFile f : list()) if (name.equals(f.getName())) return f;
        return null;
    }

    @NotNull
    @Override
    public Collection<? extends SimpleVFile> list() {
        @NotNull LinkedList<JDBCTable> tableList = new LinkedList<>();
        try {
            DatabaseMetaData dbmd = conn.getMetaData();
            @NotNull String[] types = {"TABLE"};
            ResultSet rs = dbmd.getTables(null, null, "%", types);
            while (rs.next()) {
                tableList.add(new JDBCTable(rs.getString("TABLE_NAME")));
            }
            LOG.log(Level.INFO, "Tables: {0}", tableList);
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Reading from metadata", getName());
            LOG.log(Level.SEVERE, null, e);
        }
        return tableList;
    }

    class JDBCTable extends VFSStub {

        JDBCTable(String name) {
            super(name);
        }

        @Nullable
        @Override
        public SimpleVFile get(@NotNull final String name) {
            for (@NotNull SimpleVFile f : list()) if (name.equals(f.getName())) return f;
            return null;
        }

        @NotNull
        @Override
        public Collection<? extends SimpleVFile> list() {
            @NotNull LinkedList<MockFile> rows = new LinkedList<>();
            try {
                PreparedStatement st = conn.prepareStatement(String.format("SELECT * FROM %s", getName()));
                ResultSet rs = st.executeQuery();
                int len = rs.getMetaData().getColumnCount();
                while (rs.next()) {
                    @NotNull StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < len; i++) {
                        sb.append('\t').append(rs.getString(i + 1));
                    }
                    rows.add(new MockFile(rs.getString(1), sb.substring(1)));
                }
                rs.close();
                st.close();
            } catch (SQLException e) {
                LOG.log(Level.SEVERE, "Reading from table {0}", getName());
                LOG.log(Level.SEVERE, null, e);
            }
            return rows;
        }
    }
}