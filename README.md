# OrchFlow
Orchestrator for SDN networks with multiple OpenFlow protocol

## Docker images
```
neo4j
pierrecdn/floodlight
```
## Docker deploy images
```
docker run -p 6653 -p 8080 --name=floodlight pierrecdn/floodlight
docker run -p 7474:7474 -p 7687:7687 --name=neo4j neo4j
```
