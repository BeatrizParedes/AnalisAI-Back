<img width="851" height="315" alt="capaanalisai" src="https://github.com/user-attachments/assets/df092258-bc2f-402e-962e-2301ce8e75da" />

![Java](https://img.shields.io/badge/Backend-SpringBoot-green?logo=java&logoColor=white)
![Angular](https://img.shields.io/badge/Frontend-Angular-red?logo=angular&logoColor=white)
![MariaDB](https://img.shields.io/badge/Database-MariaDB-blue?logo=mariadb&logoColor=white)
![Jira](https://img.shields.io/badge/API-Jira-lightblue?logo=jira&logoColor=white)
![AI](https://img.shields.io/badge/AI-LLMs-purple?logo=openai&logoColor=white)

O **analisAI** é uma ferramenta de **análise e planejamento estratégico** que se conecta ao **Jira**, processa dados do backlog e utiliza **IA (LLMs)** para identificar gargalos, dependências e sugerir planos de ação otimizados.  

> Diferente de ferramentas que criam tarefas, o analisAI foca na **inteligência de planejamento**: a IA analisa as tarefas já existentes e recomenda melhorias para acelerar entregas e reduzir riscos.  

---

## ✨ Funcionalidades

- 🔗 **Integração com Jira Cloud** via REST API.  
- 📊 **Dashboard interativo** (Angular) exibindo:  
  - Gargalos identificados.  
  - Dependências não óbvias.  
  - Sugestões de execução paralela.  
  - Projeções de prazos mais realistas.  
- 🧠 **Análises avançadas com LLMs**:  
  - Identificação de riscos em descrições de tarefas.  
  - Sugestões de reorganização do backlog.  
- 💾 **Armazenamento estratégico no MariaDB**: histórico de análises e recomendações.  
- ⚡ **Backend robusto com Spring Boot**:  
  - Processamento de dados do Jira.  
  - Camada de negócios para análises personalizadas.  
  - Comunicação com LLMs para geração de insights.  

---

## 🛠️ Stack Tecnológica

### Backend
- **Spring Boot** (Java)  
- **MariaDB** para armazenamento de dados  
- **Integração com Jira Cloud REST API**  
- **LLMs** para análise inteligente  

### Frontend
- **Angular**  
- **Dashboard interativo**  
- Visualização de gargalos, dependências e projeções de prazo  
- Integração direta com o backend para exibir resultados  

---

## 🚀 Como Rodar a Aplicação

### 1. Pré-requisitos
- Java 17+    
- MariaDB em execução  
- Conta Jira Cloud com API Token  
- Chave de API do provedor de LLM
- Instalar o Docker Desktop  

### 2. Rodando o Backend
```bash
# 2.1 Instale e abra o Docker Desktop

# 2.2 Clone o projeto 
https://github.com/BeatrizParedes/AnalisAI-Back.git

# 2.3 Navegue até a pasta do backend
cd AnalisAI_Backend

# 2.4 Rode o comando: 
docker compose up -d

# 2.5 Verifique o containers: 
docker ps

# 2.6 Compile e rode o backend
mvn spring-boot:run

````
### 👩‍💻 Equipe

- Beatriz Paredes 
- Catarina Loureiro
- Cecília Medeiros 
- Isabella Batista
- Maria Gabriela Damásio 
- José Leandro Morais

### 🔗Frontend do Projeto: [AnalisAI - Frontend](https://github.com/Cecimedeiros/AnalisAI-Front.git)
