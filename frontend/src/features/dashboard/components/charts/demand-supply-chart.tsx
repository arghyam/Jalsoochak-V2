import { useMemo } from 'react'
import { useTheme } from '@chakra-ui/react'
import * as echarts from 'echarts'
import { EChartsWrapper } from './echarts-wrapper'
import { getBodyText7Style } from './chart-text-style'
import type { DemandSupplyData } from '../../types'

interface DemandSupplyChartProps {
  data: DemandSupplyData[]
  className?: string
  height?: string | number
}

export function DemandSupplyChart({ data, className, height = '400px' }: DemandSupplyChartProps) {
  const theme = useTheme()
  const bodyText7 = getBodyText7Style(theme)

  const option = useMemo<echarts.EChartsOption>(() => {
    const periods = data.map((d) => d.period)
    const demand = data.map((d) => d.demand)
    const supply = data.map((d) => d.supply)

    return {
      tooltip: {
        show: false,
      },
      grid: {
        left: '8%',
        right: '4%',
        top: '14%',
        bottom: '5%',
        containLabel: true,
      },
      xAxis: {
        type: 'category',
        boundaryGap: false,
        data: periods,
        name: 'Year',
        nameLocation: 'middle',
        nameGap: 28,
        axisLine: {
          lineStyle: {
            color: '#E4E4E7',
          },
        },
        axisTick: {
          show: false,
        },
        axisLabel: {
          rotate: 0,
          fontSize: bodyText7.fontSize,
          lineHeight: bodyText7.lineHeight,
          fontWeight: 400,
          color: bodyText7.color,
        },
        nameTextStyle: {
          fontSize: bodyText7.fontSize,
          lineHeight: bodyText7.lineHeight,
          fontWeight: 400,
          color: bodyText7.color,
        },
      },
      yAxis: {
        type: 'value',
        name: 'Quantity (units)',
        nameLocation: 'middle',
        nameGap: 40,
        nameTextStyle: {
          fontSize: bodyText7.fontSize,
          lineHeight: bodyText7.lineHeight,
          fontWeight: 400,
          color: bodyText7.color,
        },
        axisLabel: {
          fontSize: bodyText7.fontSize,
          lineHeight: bodyText7.lineHeight,
          fontWeight: 400,
          color: bodyText7.color,
        },
        axisLine: {
          lineStyle: {
            color: '#E4E4E7',
          },
        },
        axisTick: {
          show: false,
        },
        splitLine: {
          lineStyle: {
            color: '#E4E4E7',
          },
        },
        interval: 25,
        max: 125,
      },
      series: [
        {
          name: 'Demand',
          type: 'line',
          data: demand,
          smooth: false,
          symbol: 'none',
          itemStyle: {
            color: '#3291D1',
          },
          lineStyle: {
            width: 2,
          },
        },
        {
          name: 'Supply',
          type: 'line',
          data: supply,
          smooth: false,
          symbol: 'none',
          itemStyle: {
            color: '#ADD3ED', // light blue
          },
          lineStyle: {
            width: 2,
          },
        },
      ],
    }
  }, [data, bodyText7])

  const containerHeight = typeof height === 'number' ? `${height}px` : height
  const legendItems = [
    { label: 'Demand', color: '#3291D1' },
    { label: 'Supply', color: '#ADD3ED' },
  ]

  return (
    <div
      className={className}
      style={{
        width: '100%',
        height: containerHeight,
        display: 'flex',
        flexDirection: 'column',
      }}
    >
      <div style={{ flex: 1, minHeight: 0 }}>
        <EChartsWrapper option={option} height="100%" />
      </div>
      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          gap: '16px',
          paddingTop: '8px',
        }}
      >
        {legendItems.map((item) => (
          <div key={item.label} style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
            <span
              aria-hidden="true"
              style={{
                width: '8px',
                height: '8px',
                borderRadius: '2px',
                backgroundColor: item.color,
                display: 'inline-block',
              }}
            />
            <span
              style={{
                fontSize: bodyText7.fontSize,
                lineHeight: `${bodyText7.lineHeight}px`,
                fontWeight: 400,
                color: bodyText7.color,
              }}
            >
              {item.label}
            </span>
          </div>
        ))}
      </div>
    </div>
  )
}
