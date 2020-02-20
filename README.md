# Simple Ktor example with Camunda

```
,--. ,--.   ,--.                      ,-----.                                          ,--.          
|  .'   / ,-'  '-.  ,---.  ,--.--.   '  .--./  ,--,--. ,--,--,--. ,--.,--. ,--,--,   ,-|  |  ,--,--. 
|  .   '  '-.  .-' | .-. | |  .--'   |  |     ' ,-.  | |        | |  ||  | |      \ ' .-. | ' ,-.  | 
|  |\   \   |  |   ' '-' ' |  |      '  '--'\ \ '-'  | |  |  |  | '  ''  ' |  ||  | \ `-' | \ '-'  | 
`--' '--'   `--'    `---'  `--'       `-----'  `--`--' `--`--`--'  `----'  `--''--'  `---'   `--`--' 
```

* Starts a camunda engine with InMemory Configuration
* Deploys automatically all *bpmn files
* Register all classes which implements JavaDelegate, TaskListener, ExecutionListener and Variable Listeners as camunda delegates

# Setup

./gradle clean package

# Run

./gradle run

# Start process

GET http://localhost:8080/process/startByKey/ktor-process
