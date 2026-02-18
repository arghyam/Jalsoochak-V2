import { useMemo } from 'react'
import { useBreakpointValue, useTheme } from '@chakra-ui/react'
import * as echarts from 'echarts'
import { EChartsWrapper } from './echarts-wrapper'
import { getBodyText7Style } from './chart-text-style'
import type { ImageSubmissionStatusData } from '../../types'

interface ImageSubmissionStatusChartProps {
  data: ImageSubmissionStatusData[]
  className?: string
  height?: string | number
}

const defaultColors = ['#3291D1', '#ADD3ED']

export function ImageSubmissionStatusChart({
  data,
  className,
  height = '406px',
}: ImageSubmissionStatusChartProps) {
  const theme = useTheme()
  const bodyText7 = getBodyText7Style(theme)
  const pieRadius = useBreakpointValue<(string | number)[]>({
    base: ['0%', '75%'],
    sm: ['0%', '70%'],
    md: ['0%', '68%'],
  }) ?? ['0%', '68%']
  const pieCenter = useBreakpointValue<[string, string]>({
    base: ['50%', '42%'],
    sm: ['50%', '45%'],
    md: ['50%', '45%'],
  }) ?? ['50%', '45%']

  const option = useMemo<echarts.EChartsOption>(() => {
    return {
      tooltip: {
        show: false,
      },
      series: [
        {
          type: 'pie',
          radius: pieRadius,
          center: pieCenter,
          avoidLabelOverlap: true,
          label: {
            show: false,
          },
          labelLine: {
            show: false,
          },
          data: data.map((entry, index) => ({
            name: entry.label,
            value: entry.value,
            itemStyle: {
              color: defaultColors[index % defaultColors.length],
            },
          })),
        },
      ],
    }
  }, [data, bodyText7, pieCenter, pieRadius])

  const containerHeight = typeof height === 'number' ? `${height}px` : height

  return (
    <div
      className={className}
      style={{
        width: '100%',
        minWidth: 0,
        height: containerHeight,
        display: 'flex',
        flexDirection: 'column',
      }}
    >
      <div style={{ flex: 1, minHeight: 0, minWidth: 0 }}>
        <EChartsWrapper option={option} height="100%" />
      </div>
      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          gap: '16px',
          paddingTop: '8px',
          flexWrap: 'wrap',
          rowGap: '6px',
        }}
      >
        {data.map((entry, index) => (
          <div key={entry.label} style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
            <span
              aria-hidden="true"
              style={{
                width: '8px',
                height: '8px',
                borderRadius: '2px',
                backgroundColor: defaultColors[index % defaultColors.length],
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
              {entry.label}
            </span>
          </div>
        ))}
      </div>
    </div>
  )
}
