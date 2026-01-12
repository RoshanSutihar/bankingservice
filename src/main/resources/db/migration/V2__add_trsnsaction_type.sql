INSERT INTO transaction_types (type_code, type_name, description, affects_balance)
VALUES
('PAYMENT', 'Payment', 'Outgoing payment transaction', true),
('REFUND', 'Refund', 'Payment refund', true),
('BILL_PAY', 'Bill Payment', 'Utility or bill payment', true),
('QR_PAY', 'QR Payment', 'QR code payment', true);