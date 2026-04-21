DROP DATABASE IF EXISTS casino;
CREATE DATABASE casino;
USE casino;


CREATE USER IF NOT EXISTS 'javauser'@'localhost' IDENTIFIED BY 'java1234';
GRANT ALL PRIVILEGES ON casino.* TO 'javauser'@'localhost';


CREATE TABLE usuarios (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    apellido VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    saldo DECIMAL(10,2) DEFAULT 0.00,
    rol ENUM('Jugador', 'Admin') DEFAULT 'Jugador'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE juegos (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL,
    reglas TEXT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE mesas (
    id INT AUTO_INCREMENT PRIMARY KEY,
    id_juego INT NOT NULL,
    apuesta_minima DECIMAL(10,2) NOT NULL,
    apuesta_maxima DECIMAL(10,2) NOT NULL,
    estado ENUM('Activa', 'Inactiva') DEFAULT 'Activa',
    
    
    FOREIGN KEY (id_juego) REFERENCES juegos(id) ON DELETE CASCADE,
    
    
    CHECK (apuesta_minima <= apuesta_maxima)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE apuestas (
    id INT AUTO_INCREMENT PRIMARY KEY,
    id_usuario INT NOT NULL,
    id_mesa INT NOT NULL,
    monto_apostado DECIMAL(10,2) NOT NULL,
    resultado_monto DECIMAL(10,2),
    fecha DATETIME DEFAULT CURRENT_TIMESTAMP,
    
    
    FOREIGN KEY (id_usuario) REFERENCES usuarios(id) ON DELETE CASCADE,
    FOREIGN KEY (id_mesa) REFERENCES mesas(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE INDEX idx_mesas_juego ON mesas(id_juego);
CREATE INDEX idx_apuestas_usuario ON apuestas(id_usuario);
CREATE INDEX idx_apuestas_mesa ON apuestas(id_mesa);


FLUSH PRIVILEGES;

-- ==========================================
-- INSERTS DE PRUEBA (DATA SEMILLA)
-- ==========================================

-- Usuarios: 1 Admin y 1 Jugador (con 500 lucas de saldo para timbear)
INSERT INTO usuarios (nombre, apellido, email, password, saldo, rol) VALUES
('Chiqui', 'Tapia', 'elchiquicenter@admin.com', 'admin123', 0.00, 'Admin'),
('Luu', 'Salvu', 'cliente@gmail.com', 'cliente123', 500000.00, 'Jugador');

-- Juegos
INSERT INTO juegos (nombre, reglas) VALUES
('Ruleta', 'Reglas clásicas de la ruleta europea (un solo 0).'),
('Blackjack', 'El croupier se planta en 17. El Blackjack paga 3 a 2.');

-- Mesas
INSERT INTO mesas (id_juego, apuesta_minima, apuesta_maxima, estado) VALUES
(1, 100.00, 10000.00, 'Activa'),  -- Mesa 1: Ruleta
(2, 500.00, 50000.00, 'Activa');  -- Mesa 2: Blackjack

-- Apuestas de prueba
INSERT INTO apuestas (id_usuario, id_mesa, monto_apostado, resultado_monto) VALUES
(2, 1, 1500.00, -1500.00), -- Lu apostó 1.5k en la Ruleta y perdió
(2, 2, 2000.00, 4000.00);  -- Lu apostó 2k en el Blackjack y ganó