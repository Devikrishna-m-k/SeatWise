-- init_mysql.sql
CREATE DATABASE IF NOT EXISTS SeatWise;
USE SeatWise;

-- Student table
CREATE TABLE IF NOT EXISTS Student (
  student_id INT PRIMARY KEY AUTO_INCREMENT,
  roll_no VARCHAR(50) UNIQUE,
  name VARCHAR(100) NOT NULL,
  branch VARCHAR(50) NOT NULL
) ENGINE=InnoDB;

-- Room table
CREATE TABLE IF NOT EXISTS Room (
  room_id INT PRIMARY KEY AUTO_INCREMENT,
  room_name VARCHAR(100),
  capacity INT NOT NULL
) ENGINE=InnoDB;

-- ExamSlot table
CREATE TABLE IF NOT EXISTS ExamSlot (
  exam_slot_id INT PRIMARY KEY AUTO_INCREMENT,
  subject VARCHAR(100) NOT NULL,
  exam_date DATE NOT NULL,
  start_time TIME NULL
) ENGINE=InnoDB;

-- User table (admins and optionally students)
CREATE TABLE IF NOT EXISTS UserAccount (
  user_id INT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(50) NOT NULL UNIQUE,
  password VARCHAR(255) NOT NULL, -- store hashed password
  role ENUM('admin','student') NOT NULL,
  student_id INT NULL,
  FOREIGN KEY (student_id) REFERENCES Student(student_id) ON DELETE SET NULL
) ENGINE=InnoDB;

-- Allocation table
CREATE TABLE IF NOT EXISTS Allocation (
  allocation_id INT PRIMARY KEY AUTO_INCREMENT,
  student_id INT NOT NULL,
  room_id INT NOT NULL,
  seat_no INT NOT NULL,
  exam_slot_id INT NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  UNIQUE (room_id, seat_no, exam_slot_id),
  FOREIGN KEY (student_id) REFERENCES Student(student_id) ON DELETE CASCADE,
  FOREIGN KEY (room_id) REFERENCES Room(room_id) ON DELETE CASCADE,
  FOREIGN KEY (exam_slot_id) REFERENCES ExamSlot(exam_slot_id) ON DELETE CASCADE
) ENGINE=InnoDB;
