import ReactECharts from 'echarts-for-react'
import { Box } from '@chakra-ui/react'

interface LineChartProps<T extends object> {
  data: T[]
  xKey: keyof T
  yKeys: (keyof T)[]
  colors?: string[]
  height?: string
  xAxisLabel?: string
  yAxisLabel?: string
  legendLabels?: string[]
}

export function LineChart<T extends object>({
  data,
  xKey,
  yKeys,
  colors = ['#3291D1', '#D92D20'],
  height = '300px',
  xAxisLabel,
  yAxisLabel,
  legendLabels,
}: LineChartProps<T>) {
  const seriesNames = yKeys.map((key, index) => legendLabels?.[index] ?? String(key))

  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'cross',
      },
    },
    legend: {
      data: seriesNames,
      bottom: 0,
      icon: 'roundRect',
      itemWidth: 8,
      itemHeight: 8,
      itemGap: 16,
      textStyle: {
        fontSize: 12,
        color: '#1C1C1C',
        padding: [0, 0, 0, 4],
      },
      padding: [0, 0, 16, 0],
    },

    grid: {
      left: '3%',
      right: '4%',
      bottom: '15%',
      top: '10%',
      containLabel: true,
    },
    xAxis: {
      type: 'category',
      data: data.map((item) => item[xKey]),
      boundaryGap: false,
      axisLine: {
        lineStyle: {
          color: '#E5E7EB',
          width: 1,
        },
      },
      axisTick: {
        show: false,
      },
      axisLabel: {
        fontSize: 12,
        color: '#1C1C1C',
      },
      name: xAxisLabel,
      nameLocation: 'middle',
      nameGap: 30,
      nameTextStyle: {
        fontSize: 12,
        color: '#1C1C1C',
      },
    },
    yAxis: {
      type: 'value',
      axisLabel: {
        fontSize: 12,
        color: '#1C1C1C',
      },
      name: yAxisLabel,
      nameLocation: 'middle',
      nameGap: 50,
      nameTextStyle: {
        fontSize: 12,
        color: '#1C1C1C',
      },
    },
    series: yKeys.map((key, index) => ({
      name: seriesNames[index],
      type: 'line',
      data: data.map((item) => item[key]),
      smooth: true,
      symbol: 'none',
      showSymbol: false,
      color: colors[index] || '#3291D1',
      lineStyle: {
        width: 2,
      },
    })),
  }

  return (
    <Box height={height} width="100%">
      <ReactECharts option={option} style={{ height: '100%', width: '100%' }} />
    </Box>
  )
}
