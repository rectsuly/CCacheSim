# CCacheSim
Cache模拟器的java界面实现

处理器访存有三种类型：读指令、读数据和写数据，给出访存的地址和类型，所设计的 Cache 的模拟器能够进行模拟这种带有 Cache 的访存行为，并能给出统计信息，如访存次数、Cache 命中次数、命中率等。

界面实现依赖Swing组件提供的JPanle，JFrame，JButton等提供的GUI。使用“监听器”模式监听各个Button的事件，从而根据具体事件执行不同方法。
