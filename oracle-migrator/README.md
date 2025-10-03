## Oracle Migrator ##

This project takes `.sql` artifacts and helps refactor them into Java classes using Spring Boot + JPA.

---

## Setup ##

## GitHub Copilot Extension ##
1. Install **Visual Studio Code**.
2. Install the **GitHub Copilot** extension.
3. Open this project folder in VS Code.
5. Create symbolic link of file oracle-migrator.chatmode.md with command below:
ln -s /home/${user}/dev/chart-society/oracle-migrator/chatmode/oracle-migrator.chatmode.md /home/${user}/.config/Code/User/prompts/

## Github Copilot CLI ##
## Requirements ##
   - node 22 or later
   - npm 10 or later

## Instructions ##
1. npm install -g @github/copilot // TODO: find a way to install permanent
---

## How to Use ##

## Copilot Extension ##
1. Place your `.sql` files (Oracle PL/SQL procedures/packages) into the `input/` folder.
2. Open one of them in the editor (for pass context to copilot).
3. From the Copilot chat:
   - Select the oracle-migrator chatmod.
   - Ask: Refactor procedure to java classes
4. The copilot will generate Java code based on your rules and save into `output/`.

## Alternative ##
1. Place your `.sql` files (Oracle PL/SQL procedures/packages) into the `input/` folder.
2. Grant permission to execute for script task.sh
   - chmod + x task.sh
3. Run script bash to refactor procedure to java classes
   - ./task.sh
4. The copilot will generate Java code based on your rules and save into `output/`.

## Copilot CLI ##

1. Enter in oracle-migrator directory
2. Execute command on terminal:
copilot -p "Use @chatmode/oracle-migrator.chatmode.md. Refactor all files in @input/ into Java classes and write them into the @output/ folder." --allow-all-tools

** Grant permissions if needed using command below
chmod u+rw <file-or-directory>

---