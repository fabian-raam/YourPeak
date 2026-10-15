/*
SQLyog Ultimate v13.1.1 (64 bit)
MySQL - 10.4.32-MariaDB : Database - yourpeak_db
*********************************************************************
*/

/*!40101 SET NAMES utf8 */;

/*!40101 SET SQL_MODE=''*/;

/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
CREATE DATABASE /*!32312 IF NOT EXISTS*/`yourpeak_db` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci */;

USE `yourpeak_db`;

/*Table structure for table `cliente` */

CREATE TABLE `cliente` (
  `id_cliente` bigint(20) NOT NULL AUTO_INCREMENT,
  `id_usuario` bigint(20) NOT NULL,
  `id_tipo_estado` int(11) NOT NULL,
  `id_tipo_objetivo` int(11) DEFAULT NULL,
  `id_nivel_experiencia` int(11) DEFAULT NULL,
  `id_nivel_actividad` int(11) DEFAULT NULL,
  `fecha_alta` datetime NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`id_cliente`),
  UNIQUE KEY `uq_cliente_usuario` (`id_usuario`),
  KEY `fk_cliente_estado` (`id_tipo_estado`),
  KEY `fk_cliente_objetivo` (`id_tipo_objetivo`),
  KEY `fk_cliente_experiencia` (`id_nivel_experiencia`),
  KEY `fk_cliente_actividad` (`id_nivel_actividad`),
  CONSTRAINT `fk_cliente_actividad` FOREIGN KEY (`id_nivel_actividad`) REFERENCES `nivel_actividad` (`id_nivel_actividad`),
  CONSTRAINT `fk_cliente_estado` FOREIGN KEY (`id_tipo_estado`) REFERENCES `tipo_estado` (`id_tipo_estado`),
  CONSTRAINT `fk_cliente_experiencia` FOREIGN KEY (`id_nivel_experiencia`) REFERENCES `nivel_experiencia` (`id_nivel_experiencia`),
  CONSTRAINT `fk_cliente_objetivo` FOREIGN KEY (`id_tipo_objetivo`) REFERENCES `tipo_objetivo` (`id_tipo_objetivo`),
  CONSTRAINT `fk_cliente_usuario` FOREIGN KEY (`id_usuario`) REFERENCES `usuario` (`id_usuario`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

/*Table structure for table `nivel_actividad` */

CREATE TABLE `nivel_actividad` (
  `id_nivel_actividad` int(11) NOT NULL AUTO_INCREMENT,
  `nombre` varchar(30) NOT NULL,
  `descripcion` varchar(150) DEFAULT NULL,
  PRIMARY KEY (`id_nivel_actividad`),
  UNIQUE KEY `uq_nivel_actividad_nombre` (`nombre`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

/*Table structure for table `nivel_experiencia` */

CREATE TABLE `nivel_experiencia` (
  `id_nivel_experiencia` int(11) NOT NULL AUTO_INCREMENT,
  `nombre` varchar(30) NOT NULL,
  `descripcion` varchar(150) DEFAULT NULL,
  PRIMARY KEY (`id_nivel_experiencia`),
  UNIQUE KEY `uq_nivel_experiencia_nombre` (`nombre`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

/*Table structure for table `tipo_estado` */

CREATE TABLE `tipo_estado` (
  `id_tipo_estado` int(11) NOT NULL AUTO_INCREMENT,
  `nombre` varchar(30) NOT NULL,
  `descripcion` varchar(150) DEFAULT NULL,
  PRIMARY KEY (`id_tipo_estado`),
  UNIQUE KEY `uq_tipo_estado_nombre` (`nombre`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

/*Table structure for table `tipo_objetivo` */

CREATE TABLE `tipo_objetivo` (
  `id_tipo_objetivo` int(11) NOT NULL AUTO_INCREMENT,
  `nombre` varchar(50) NOT NULL,
  `descripcion` varchar(150) DEFAULT NULL,
  PRIMARY KEY (`id_tipo_objetivo`),
  UNIQUE KEY `uq_tipo_objetivo_nombre` (`nombre`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

/*Table structure for table `usuario` */

CREATE TABLE `usuario` (
  `id_usuario` bigint(20) NOT NULL AUTO_INCREMENT,
  `fecha_nacimiento` date DEFAULT NULL,
  `fecha_creacion` datetime NOT NULL DEFAULT current_timestamp(),
  `nombre` varchar(100) NOT NULL,
  `apellido` varchar(100) NOT NULL,
  `correo` varchar(254) NOT NULL,
  `contrasena_hash` varchar(255) NOT NULL,
  PRIMARY KEY (`id_usuario`),
  UNIQUE KEY `uq_usuario_correo` (`correo`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
