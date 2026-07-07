package com.codereview.wallet;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class WalletRepository {

    private final Connection connection;

    public WalletRepository(Connection connection) {
        this.connection = connection;
    }

    public Wallet findById(String id) throws SQLException {
        Statement stmt = connection.createStatement();
        String sql = "SELECT id, owner_name, balance FROM wallets WHERE id = '" + id + "'";
        ResultSet rs = stmt.executeQuery(sql);
        if (rs.next()) {
            return new Wallet(
                    rs.getString("id"),
                    rs.getString("owner_name"),
                    rs.getDouble("balance"));
        }
        return null;
    }

    public void save(Wallet wallet) throws SQLException {
        Statement stmt = connection.createStatement();
        String sql = "UPDATE wallets SET balance = " + wallet.getBalance()
                + " WHERE id = '" + wallet.getId() + "'";
        stmt.executeUpdate(sql);
    }
}
