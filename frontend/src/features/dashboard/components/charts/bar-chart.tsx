import { useMemo } from 'react'
import * as echarts from 'echarts'
import { EChartsWrapper } from './echarts-wrapper'
import type { EntityPerformance } from '../../types'

interface BarChartProps {
  data: EntityPerformance[]
  metric: 'regularity' | 'continuity'
  title: string
  className?: string
  height?: string | number
}

export function BarChart({ data, metric, title, className, height = '400px' }: BarChartProps) {
  const option = useMemo<echarts.EChartsOption>(() => {
    const sortedData = [...data].sort((a, b) => b[metric] - a[metric])

    const entities = sortedData.map((d) => d.name)
    const values = sortedData.map((d) => d[metric])

    // Color based on status
    const colors = sortedData.map((d) => {
      switch (d.status) {
        case 'good':
          return '#22c55e'
        case 'needs-attention':
          return '#f97316'
        case 'critical':
          return '#ef4444'
        default:
          return '#94a3b8'
      }
    })

    return {
      title: {
        text: title,
        left: 'center',
        textStyle: {
          fontSize: 16,
          fontWeight: 'bold',
        },
      },
      tooltip: {
        trigger: 'axis',
        axisPointer: {
          type: 'shadow',
        },
        formatter: (params: unknown) => {
          const p = params as Array<{
            name: string
            value: number
            dataIndex: number
          }>
          const param = p[0]
          const entity = sortedData[param.dataIndex]
          return `
            <div style="padding: 8px;">
              <strong>${param.name}</strong><br/>
              ${metric === 'regularity' ? 'Regularity' : 'Continuity'}: ${param.value}%<br/>
              Status: ${entity.status}
            </div>
          `
        },
      },
      grid: {
        left: '3%',
        right: '4%',
        bottom: '15%',
        containLabel: true,
      },
      xAxis: {
        type: 'category',
        data: entities,
        axisLabel: {
          rotate: 45,
          interval: 0,
        },
      },
      yAxis: {
        type: 'value',
        name: metric === 'regularity' ? 'Regularity %' : 'Continuity Index',
        nameLocation: 'middle',
        nameGap: 50,
        max: 100,
      },
      series: [
        {
          type: 'bar',
          data: values.map((value, index) => ({
            value,
            itemStyle: {
              color: colors[index],
            },
          })),
          label: {
            show: true,
            position: 'top',
            formatter: '{c}%',
          },
        },
      ],
    }
  }, [data, metric, title])

  return <EChartsWrapper option={option} className={className} height={height} />
}
