# redismq-stream-spring-boot-start
基于redis stream 实现的消息队列，快速和springboot集成
## [文档](https://www.jianshu.com/p/b95a265f838a)

## 功能特性

- [x] [异步](#异步)
- [x] [订阅发布](#订阅发布)
- [x] [延迟消息](#延迟消息)
- [x] [ACK](#ACK)
- [ ] [死信](#死信)
- [x] [优雅停机](#优雅停机)


# 延迟消息
等待一段时间后再发触发，可以先将消息存入 zSet, 定时轮询，满足时间戳的情况再触发。是否需要额外考虑 定时任务 三高问题
# 死信
当一段消息多次无法消费 加入死信队列，单独处理。


# 介绍
core 核心代码
群组默认取spring.application.name
相同群组只能消费一次
不同群组可以消费多次

common 测试通用类 包含消息实体

consumer 测试消费者 多开测试异步和订阅通知

producer 测试生产者 启动后生成消息，同时自己也在消费

