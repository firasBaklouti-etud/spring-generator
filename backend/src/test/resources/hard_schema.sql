-- USERS TABLE
CREATE TABLE users (
                       id BIGINT PRIMARY KEY,
                       username VARCHAR(100) NOT NULL UNIQUE,
                       email VARCHAR(255) NOT NULL,
                       status VARCHAR(20) DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'BANNED', 'DELETED')),
                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ROLES TABLE
CREATE TABLE roles (
                       id BIGINT PRIMARY KEY,
                       name VARCHAR(100) NOT NULL UNIQUE
);

-- USER_ROLES (Many-to-Many with composite PK)
CREATE TABLE user_roles (
                            user_id BIGINT NOT NULL,
                            role_id BIGINT NOT NULL,
                            assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            PRIMARY KEY (user_id, role_id),
                            CONSTRAINT fk_ur_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                            CONSTRAINT fk_ur_role FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
);

-- PRODUCTS TABLE
CREATE TABLE products (
                          id BIGINT PRIMARY KEY,
                          name VARCHAR(255) NOT NULL,
                          price DECIMAL(10, 2) NOT NULL CHECK (price >= 0),
                          stock INT DEFAULT 0 CHECK (stock >= 0),
                          created_by BIGINT,
                          CONSTRAINT fk_product_user FOREIGN KEY (created_by) REFERENCES users(id)
);

-- ORDERS TABLE
CREATE TABLE orders (
                        id BIGINT PRIMARY KEY,
                        user_id BIGINT NOT NULL,
                        status VARCHAR(20) DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'PAID', 'SHIPPED')),
                        total DECIMAL(12, 2) NOT NULL,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        CONSTRAINT fk_orders_user FOREIGN KEY (user_id) REFERENCES users(id)
);

-- ORDER_ITEMS TABLE
CREATE TABLE order_items (
                             id BIGINT PRIMARY KEY,
                             order_id BIGINT NOT NULL,
                             product_id BIGINT NOT NULL,
                             quantity INT NOT NULL CHECK (quantity > 0),
                             price_at_purchase DECIMAL(10, 2) NOT NULL,
                             CONSTRAINT fk_oi_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
                             CONSTRAINT fk_oi_product FOREIGN KEY (product_id) REFERENCES products(id)
);
