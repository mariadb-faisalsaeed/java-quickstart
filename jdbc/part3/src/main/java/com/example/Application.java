package com.example;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;
import java.util.Random;

public class Application {

	private static HikariDataSource dataSource;

	public static void main(String[] args) throws SQLException, IOException {
		BufferedReader reader = new BufferedReader(
            new InputStreamReader(System.in));
		
		try {
			initDatabaseConnectionPool();
			int ConnectionsToCreate=1;
			String response;
			while (ConnectionsToCreate > 0) {
				System.out.print("\n\nHow many connections to create...: ");
				response = reader.readLine();
				if (response.equals("T")) {
					listThreads();
				} else {
					ConnectionsToCreate = Integer.parseInt(response);
					for (int i=0; i<ConnectionsToCreate; i++) {
						Thread thread = new Thread(new ConnectionThread(dataSource));
            			thread.start();
					}
				}
			}
		} finally {
			closeDatabaseConnectionPool();
		}
	}

	public static void listThreads() {
		Map<Thread, StackTraceElement[]> threads = Thread.getAllStackTraces();
		for (Thread thread : threads.keySet()) {
			System.out.println("Thread name: " + thread.getName());
		}
	}

	private static class ConnectionThread implements Runnable {
        private final HikariDataSource dataSource;

        public ConnectionThread(HikariDataSource dataSource) {
            this.dataSource = dataSource;
        }

        @Override
        public void run() {
			Random rand = new Random(); 
            try (Connection con = dataSource.getConnection()) {
                System.out.println("Thread " + Thread.currentThread().getId() + " acquired a new connection.");
				for (int i=0;i<100;i++) {
					Thread.sleep(rand.nextInt(1000));
					createData(Thread.currentThread().getName(), Thread.currentThread().getId(), con);
				}
				System.out.println("Connection Closed for thread " + Thread.currentThread().getId() + "...");
				con.close();
           } catch (SQLException e) {
                System.err.println("Thread " + Thread.currentThread().getId() + " encountered an error acquiring a connection: " + e.getMessage());
            } catch (InterruptedException e) {
				e.printStackTrace();
			} finally {
				System.out.println("Thread " + Thread.currentThread().getId() + " Cleaned Up!");
			}
        }
	}

	private static void createData(String name, long threadId, Connection connection) throws SQLException {
		try (PreparedStatement statement = connection.prepareStatement("""
					INSERT INTO t(c, threadId)
					VALUES (?, ?)
				""")) {
			statement.setString(1, name);
			statement.setLong(2, threadId);
			statement.executeUpdate();
			System.out.println("Inserting for " + name + " -> " + threadId);
		}
	}

	private static void initDatabaseConnectionPool() {
		HikariConfig hikariConfig = new HikariConfig("/database.properties");
		dataSource = new HikariDataSource(hikariConfig);
		dataSource.setIdleTimeout(10000);
		dataSource.setMinimumIdle(5);
	}

	private static void closeDatabaseConnectionPool() {
		dataSource.close();
	}
}
