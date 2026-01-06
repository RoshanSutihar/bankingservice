INSERT INTO transaction_types (type_code, type_name, description, affects_balance) VALUES
('DEPOSIT', 'Deposit', 'Funds deposited to account', true),
('WITHDRAWAL', 'Withdrawal', 'Funds withdrawn from account', true),
('TRANSFER', 'Transfer', 'Transfer between accounts', true),
('FEE', 'Fee', 'Account maintenance fee', true),
('INTEREST', 'Interest', 'Interest payment', true);