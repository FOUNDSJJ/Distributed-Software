# JMeter Traffic Governance Guide

## Targets

1. Gateway dynamic discovery route
   - `http://localhost:8080/auth-service/api/products/info`
2. Gateway normal route
   - `http://localhost/api/products/info`
3. Gateway traffic governance demo
   - `http://localhost/api/traffic/unstable`
4. Dynamic config demo
   - `http://localhost/api/config/demo`

## Suggested verification flow

1. Start the stack:
   - `docker compose up -d --build`
2. Open Nacos:
   - `http://localhost:8848/nacos`
3. Verify registration in Nacos:
   - `auth-service`
   - `order-service`
   - `inventory-service`
   - `gateway-service`
4. Verify dynamic route:
   - `http://localhost:8080/auth-service/api/products/info`
5. Verify normal gateway route:
   - `http://localhost/api/products/info`
6. Verify dynamic config:
   - edit `demo.message` or `traffic.demo.delay-millis` in `auth-service.yaml`
   - publish the config
   - call `http://localhost/api/config/demo`

## Traffic governance scenarios

1. Rate limiting
   - Use the JMeter plan `traffic-governance-test-plan.jmx`
   - Hit `http://localhost/api/traffic/unstable`
   - Expect part of the requests to return `429`
2. Service degradation
   - In Nacos, set `traffic.demo.force-failure=true`
   - Publish config
   - Call `http://localhost/api/traffic/unstable`
   - Expect `status=DEGRADED`
3. Gateway fallback
   - In Nacos, set `traffic.demo.delay-millis=4000`
   - Publish config
   - Call `http://localhost/api/traffic/unstable`
   - Expect gateway `503` fallback response
4. Circuit breaker recovery
   - Restore `traffic.demo.force-failure=false`
   - Restore `traffic.demo.delay-millis=0`
   - Wait about 10 seconds
   - Call the same API again

## JMeter plan

- File: `Back_End/JMeter/traffic-governance-test-plan.jmx`
- Default target:
  - protocol: `http`
  - host: `localhost`
  - port: `80`
  - path: `/api/traffic/unstable`
