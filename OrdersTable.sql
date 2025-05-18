create database Retail_Store;

use Retail_Store;


CREATE TABLE Orders (
    id INT AUTO_INCREMENT PRIMARY KEY,
    timestamp VARCHAR(255),
    product_name VARCHAR(255),
    expiry_date VARCHAR(255),
    quantity INT,
    unit_price FLOAT,
    channel VARCHAR(50),
    payment_method VARCHAR(50),
    final_price FLOAT
);


select * from Orders;




delete from Orders;