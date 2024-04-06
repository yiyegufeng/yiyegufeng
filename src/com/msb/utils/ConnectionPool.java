package com.msb.utils;

import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

public class ConnectionPool {

    //定义List集合，存放Connection对象
    private static List<Connection> list= new LinkedList<>();

    static {
        try {

            //把属性文件读取
            InputStream input = ConnectionPool.class.getResourceAsStream("/db.properties");

            //创建Properties对象
            Properties  properties = new Properties();
            //使用Properties对象读取属性文件并存储键值对
            properties.load(input);

            Class.forName(properties.getProperty("driver"));
			//产生10个Connection对象，并保存到List集合中，我们以后写代码不用每次都自己创建Connection对象，直接从池子中取出就可以了
            for (int i = 0; i < 10; i++) {
                Connection conn = DriverManager.getConnection(
                        properties.getProperty("url"),
                        properties.getProperty("username"),
                        properties.getProperty("password"));
                list.add(conn);
            }

        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SQLException | IOException e) {
            e.printStackTrace();
        }

    }


    //获得Connection对象
	//3. 方法加入synchronized修饰，代表加锁，谁是锁：当前类ConnectionPool的字节码对象：
	//4. 意味着你调用的时候池子如果没有Connection对象你就等待,因为：wait进入阻塞状态但是同时释放了锁
    public synchronized static Connection getConnection(){

        //2.如果池子中都移除了，代表池子中没有Connection对象
        if(list.size() == 0){
            try {
                ConnectionPool.class.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
		//1.获取就是从池子中移除，拿出去，用remove，拿出去后返回给我，方法返回值为conn
        Connection conn = list.remove(0);
        return conn;

    }

    //把Conn对象放回池子
    public synchronized static void close(Connection connection, PreparedStatement pstmt, ResultSet rs){

        if(rs != null){
            try {
                rs.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        if(pstmt != null){
            try {
                pstmt.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
		//关闭的意思就是这个connection对象，重新放回池子
        list.add(connection);
        //随机唤醒一个线程
        ConnectionPool.class.notify();

    }

	/*
	如果一个线程调用了某个对象的wait方法，那么该线程进入到该对象的等待池中(并且已经将锁释放)，
	如果未来的某一时刻，另外一个线程调用了相同对象的notify方法，
	那么该等待池中的线程就会被唤起，然后进入到对象的锁池里面去获得该对象的锁，
	如果获得锁成功后，那么该线程就会沿着wait方法之后的路径继续执行,注意是沿着wait方法之后。

	*/

}
