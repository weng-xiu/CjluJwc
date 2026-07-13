UPDATE sys_user SET password='$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2' WHERE user_name='admin';
SELECT user_name, password FROM sys_user WHERE user_name='admin';
