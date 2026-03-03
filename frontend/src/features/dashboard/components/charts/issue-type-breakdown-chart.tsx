import { useMemo } from 'react'
import { useTheme } from '@chakra-ui/react'
import * as echarts from 'echarts'
import { EChartsWrapper } from './echarts-wrapper'
import { getBodyText7Style } from './chart-text-style'
import type { WaterSupplyOutageData } from '../../types'

interface IssueTypeBreakdownChartProps {
  data: WaterSupplyOutageData[]
  className?: string
  height?: string | number
}

const outageColors = {
  electricityFailure: '#D6E9F6',
  pipelineLeak: '#ADD3ED',
  pumpFailure: '#84BDE3',
  valveIssue: '#3291D1',
  sourceDrying: '#1E577D',
}

export function IssueTypeBreakdownChart({
  data,
  className,
  height = '400px',
}: IssueTypeBreakdownChartProps) {
  const theme = useTheme()
  const bodyText7 = getBodyText7Style(theme)

  const totals = useMemo(
    () =>
      data.reduce(
        (acc, entry) => ({
          electricityFailure: acc.electricityFailure + entry.electricityFailure,
          pipelineLeak: acc.pipelineLeak + entry.pipelineLeak,
          pumpFailure: acc.pumpFailure + entry.pumpFailure,
          valveIssue: acc.valveIssue + entry.valveIssue,
          sourceDrying: acc.sourceDrying + entry.sourceDrying,
        }),
        {
          electricityFailure: 0,
          pipelineLeak: 0,
          pumpFailure: 0,
          valveIssue: 0,
          sourceDrying: 0,
        }
      ),
    [data]
  )

  const option = useMemo<echarts.EChartsOption>(() => {
    return {
      tooltip: {
        show: false,
      },
      series: [
        {
          type: 'pie',
          radius: ['0%', '70%'],
          center: ['50%', '45%'],
          avoidLabelOverlap: true,
          label: {
            show: false,
          },
          labelLine: {
            show: false,
          },
          data: [
            {
              name: 'Electrical failure',
              value: totals.electricityFailure,
              itemStyle: { color: outageColors.electricityFailure },
            },
            {
              name: 'Pipeline break',
              value: totals.pipelineLeak,
              itemStyle: { color: outageColors.pipelineLeak },
            },
            {
              name: 'Pump failure',
              value: totals.pumpFailure,
              itemStyle: { color: outageColors.pumpFailure },
            },
            {
              name: 'Valve issue',
              value: totals.valveIssue,
              itemStyle: { color: outageColors.valveIssue },
            },
            {
              name: 'Source Drying',
              value: totals.sourceDrying,
              itemStyle: { color: outageColors.sourceDrying },
            },
          ],
        },
      ],
    }
  }, [totals])

  const containerHeight = typeof height === 'number' ? `${height}px` : height
  const legendItems = [
    { label: 'Electrical failure', color: outageColors.electricityFailure },
    { label: 'Pipeline break', color: outageColors.pipelineLeak },
    { label: 'Pump failure', color: outageColors.pumpFailure },
    { label: 'Valve issue', color: outageColors.valveIssue },
    { label: 'Source Drying', color: outageColors.sourceDrying },
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
          flexWrap: 'wrap',
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
