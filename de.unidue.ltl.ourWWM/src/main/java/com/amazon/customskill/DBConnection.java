package com.amazon.customskill;

import java.sql.DriverManager;
import java.sql.*;

public class DBConnection {

	static String DBName = "AlexaBeispiel.db";
	private static Connection con = null;
/*
 * establishing the connection with the SQLite database 
 * */
	public static Connection getConnection() {
		try {
			Class.forName("org.sqlite.JDBC");
			try {
				con = DriverManager.getConnection("jdbc:sqlite:/Users/andrea/git/Praxisprojekt/de.unidue.ltl.ourWWM/src/main/resources/" + DBName);
			} catch (SQLException ex) {
				System.out.println("Failed to create the database connection.");
			}
		} catch (ClassNotFoundException ex) {
			System.out.println("Driver not found.");
		}
		return con;
	}

}