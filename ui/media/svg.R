library(ggplot2)

N <- 300
data<-data.frame(Vehicles=seq(1:N), Minutes=jitter(sort(20+100*rbeta(N,5,3))))
gg<- ggplot(data, aes(x=Minutes, y=Vehicles)) +
  theme(aspect.ratio=5/7) +
  xlab("Minutes") +
  ylab("Vehicles Count") +
  ggtitle("Harcourt Safe Line") + 
  geom_point(aes(colour=data$Vehicles)) +
  scale_colour_gradient("", low = "#ffcc00", high = "#99cc00")
show(gg)