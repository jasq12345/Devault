CREATE DATABASE auth_db;
CREATE DATABASE workspace_db;

CREATE USER auth_user WITH PASSWORD 'auth_password';
CREATE USER workspace_user WITH PASSWORD 'workspace_password';

GRANT ALL PRIVILEGES ON DATABASE auth_db TO auth_user;
GRANT ALL PRIVILEGES ON DATABASE workspace_db TO workspace_user;

\c auth_db
GRANT ALL ON SCHEMA public TO auth_user;

\c workspace_db
GRANT ALL ON SCHEMA public TO workspace_user;