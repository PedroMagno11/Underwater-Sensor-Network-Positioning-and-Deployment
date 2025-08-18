# Underwater Sensor Network Positioning and Deployment

## 📌 Descrição do Projeto
Este projeto é um **Algoritmos Genéticos (AGs)** para otimizar o **posicionamento de sensores subaquáticos (boias)**, capazes de detectar explosões (splash) tanto das Granadas GAE e EXSUP, e além disso calcular com maior precisão o **ponto de queda**.

A pesquisa explora o uso de técnicas de IA para cenários de defesa e aplicações navais, avaliando métricas como **custo, precisão e robustez do arranjo de sensores**.

---

## ⚙️ Tecnologias Utilizadas

* **Java 21**
* **Maven** (gerenciamento de dependências)
* **Jackson** (serialização JSON)
* **JavaFX** (visualização e interface experimental)

---

## 🧬 Conceitos Principais

* **Indivíduo** → Representa um arranjo de boias.
* **Gene (Boia)** → Cada boia é caracterizada por posição cartesiana e coordenadas geográficas.
* **Fitness** → Calculado com base no custo/erro de estimativa do ponto de queda.
* **Algoritmo Genético** → Evolui populações de arranjos de boias usando:

    * Seleção
    * Crossover
    * Mutação
    * Elitismo

---

## 🚀 Como Executar

### Pré-requisitos

* JDK **21** instalado
* **Maven 3.8+** instalado

### Rodando o projeto

```bash
# Clonar repositório
git clone https://github.com/PedroMagno11/Underwater-Sensor-Network-Positioning-and-Deployment
cd Underwater-Sensor-Network-Positioning-and-Deployment

# Compilar e rodar
mvn clean package
java -jar target/AlgoritmoGenetico-1.0-SNAPSHOT.jar
```


## 📊 Resultados Esperados

* Redução do **erro médio** na estimativa da posição de impacto.
* Avaliação de diferentes tamanhos de população e número de gerações.
* Comparação de estratégias de crossover, mutação e elitismo.
* Análise de custo computacional vs. precisão obtida.

---

## 🧪 Próximos Passos

* 🎥 Criar interface visual para simulação (JavaFX).

---

## 👨‍💻 Autores

- **Pedro Magno**

- **Alexandre Francilino**
