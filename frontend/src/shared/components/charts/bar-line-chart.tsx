import ReactECharts from 'echarts-for-react'
import { Box, useBreakpointValue } from '@chakra-ui/react'

interface BarLineChartProps<T extends object> {
  data: T[]
  xKey: keyof T
  barKey: keyof T
  lineKey: keyof T
  barColor?: string
  lineColor?: string
  height?: string
  barLegendLabel?: string
  lineLegendLabel?: string
  lineAccentColor?: string
}

export function BarLineChart<T extends object>({
  data,
  xKey,
  barKey,
  lineKey,
  barColor = '#3291D1',
  lineColor = '#FFA100',
  height = '400px',
  barLegendLabel,
  lineLegendLabel,
  lineAccentColor = '#FFECCC',
}: BarLineChartProps<T>) {
  const legendData = [barLegendLabel || String(barKey), lineLegendLabel || String(lineKey)]

  // Responsive settings
  const fontSize = useBreakpointValue({ base: 10, md: 12 }) ?? 12
  const labelRotate = useBreakpointValue({ base: 45, md: 0 }) ?? 0
  const chartHeight = useBreakpointValue({ base: '280px', md: height }) ?? height

  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'cross',
      },
    },
    legend: {
      data: legendData,
      bottom: 0,
      icon: 'square',
      textStyle: {
        fontSize,
        color: '#1C1C1C',
      },
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: labelRotate > 0 ? '20%' : '15%',
      top: '10%',
      containLabel: true,
    },
    xAxis: {
      type: 'category',
      data: data.map((item) => item[xKey]),
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
        fontSize,
        color: '#1C1C1C',
        rotate: labelRotate,
      },
    },
    yAxis: [
      {
        type: 'value',
        position: 'left',
        axisLabel: {
          fontSize: 12,
          color: '#1C1C1C',
        },
        splitLine: {
          lineStyle: {
            color: '#E5E7EB',
            type: 'dashed',
          },
        },
      },
      {
        type: 'value',
        position: 'right',
        axisLabel: {
          fontSize: 12,
          color: '#1C1C1C',
        },
        splitLine: {
          show: false,
        },
      },
    ],
    series: [
      {
        name: lineLegendLabel || String(lineKey),
        type: 'line',
        yAxisIndex: 1,
        z: 1,
        data: data.map((item) => item[lineKey]),
        smooth: true,
        symbol: 'none',
        showSymbol: false,
        lineStyle: {
          width: 2,
          color: lineColor,
        },
        itemStyle: {
          color: lineAccentColor,
        },
        legendHoverLink: true,

        emphasis: {
          itemStyle: {
            color: lineAccentColor,
          },
        },
        areaStyle: {
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [
              {
                offset: 0,
                color: lineAccentColor,
              },
              {
                offset: 1,
                color: lineAccentColor,
              },
            ],
          },
        },
      },
      {
        name: barLegendLabel || String(barKey),
        type: 'bar',
        z: 2,
        data: data.map((item) => item[barKey]),
        itemStyle: {
          color: barColor,
          borderRadius: [12, 12, 12, 12],
        },
        barCategoryGap: '40%',
      },
    ],
  }

  return (
    <Box
      height={chartHeight}
      width="100%"
      role="img"
      aria-label={`Chart showing ${barLegendLabel || String(barKey)} and ${lineLegendLabel || String(lineKey)}`}
    >
      <ReactECharts option={option} style={{ height: '100%', width: '100%' }} />
    </Box>
  )
}
