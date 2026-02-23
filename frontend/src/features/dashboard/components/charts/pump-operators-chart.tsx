import { useMemo } from 'react'
import { useBreakpointValue, useTheme } from '@chakra-ui/react'
import * as echarts from 'echarts'
import { EChartsWrapper } from './echarts-wrapper'
import { getBodyText7Style } from './chart-text-style'
import type { PumpOperatorsData } from '../../types'

interface PumpOperatorsChartProps {
  data: PumpOperatorsData[]
  className?: string
  height?: string | number
  note?: string
}

const defaultColors = ['#3291D1', '#ADD3ED']

export function PumpOperatorsChart({
  data,
  className,
  height = '360px',
  note,
}: PumpOperatorsChartProps) {
  const theme = useTheme()
  const bodyText7 = getBodyText7Style(theme)
  const noteColor = theme?.colors?.neutral?.['950'] ?? bodyText7.color ?? '#667085'
  const pieRadius = useBreakpointValue<(string | number)[]>({
    base: ['50%', '75%'],
    sm: ['50%', '85%'],
    md: ['50%', '85%'],
  }) ?? ['50%', '85%']
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
          startAngle: 360,
          clockwise: true,
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
  }, [data, pieCenter, pieRadius])

  const containerHeight = typeof height === 'number' ? `${height}px` : height
  const chartSize = 300

  return (
    <div
      className={className}
      style={{
        width: '100%',
        height: containerHeight,
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
      }}
    >
      <div
        style={{
          width: '100%',
          maxWidth: '594px',
          height: '336px',
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          justifyContent: 'center',
          gap: '12px',
        }}
      >
        <div
          style={{
            width: `${chartSize}px`,
            height: `${chartSize}px`,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
          }}
        >
          <EChartsWrapper option={option} height="100%" />
        </div>
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            gap: '16px',
            width: '100%',
          }}
        >
          {data.map((entry, index) => (
            <div
              key={`${entry.label}-${index}`}
              style={{ display: 'flex', alignItems: 'center', gap: '4px' }}
            >
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
      {note ? (
        <div
          style={{
            fontSize: bodyText7.fontSize,
            lineHeight: `${bodyText7.lineHeight}px`,
            fontWeight: 400,
            color: noteColor,
            textAlign: 'left',
            paddingTop: '40px',
            width: '100%',
            paddingBottom: '0px',
          }}
        >
          {note}
        </div>
      ) : null}
    </div>
  )
}
