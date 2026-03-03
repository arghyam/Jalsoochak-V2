import { useMemo } from 'react'
import { useTheme } from '@chakra-ui/react'
import * as echarts from 'echarts'
import { EChartsWrapper } from './echarts-wrapper'
import { getBodyText6Style } from './chart-text-style'
import type { EntityPerformance } from '../../types'

interface IndiaMapChartProps {
  data: EntityPerformance[]
  onStateClick?: (stateId: string, stateName: string) => void
  onStateHover?: (stateId: string, stateName: string, metrics: EntityPerformance) => void
  className?: string
  height?: string | number
}

// Note: Color mapping is handled by visualMap in ECharts option

export function IndiaMapChart({
  data,
  onStateClick,
  onStateHover,
  className,
  height = '600px',
}: IndiaMapChartProps) {
  const theme = useTheme()

  const option = useMemo<echarts.EChartsOption>(() => {
    // Create map data series
    const mapSeries = data.map((state) => ({
      name: state.name,
      value: state.compositeScore,
      stateId: state.id,
      status: state.status,
      metrics: {
        coverage: state.coverage,
        regularity: state.regularity,
        continuity: state.continuity,
        quantity: state.quantity,
      },
    }))

    return {
      backgroundColor: '#FAFAFA',
      title: {
        left: 'center',
        textStyle: {
          fontSize: 18,
          fontWeight: 'bold',
        },
      },
      tooltip: {
        trigger: 'item',
        formatter: (params: unknown) => {
          const p = params as {
            data?: {
              name: string
              value: number
              metrics: {
                coverage: number
                regularity: number
                continuity: number
                quantity: number
              }
            }
          }
          if (p.data) {
            const { name, value, metrics } = p.data
            return `
              <div style="padding: 8px;">
                <strong>${name}</strong><br/>
                Composite Score: ${value.toFixed(2)}<br/>
                Coverage: ${metrics.coverage.toFixed(1)}%<br/>
                Regularity: ${metrics.regularity.toFixed(1)}%<br/>
                Continuity: ${metrics.continuity.toFixed(1)}<br/>
                Quantity: ${metrics.quantity} LPCD
              </div>
            `
          }
          return (p as { name?: string }).name || ''
        },
      },
      series: [
        {
          name: 'State Performance',
          type: 'map',
          map: 'india', // Requires India GeoJSON to be registered via registerIndiaMap()
          roam: true,
          // Note: If map is not registered, ECharts will show an error
          // Register the map using: registerIndiaMap(geoJsonData) from utils/map-registry
          label: {
            show: true,
            fontSize: 10,
          },
          data: mapSeries,
          itemStyle: {
            areaColor: '#3291D1',
            borderColor: '#fff',
            borderWidth: 1,
          },
          emphasis: {
            itemStyle: {
              areaColor: '#2874A7',
              borderWidth: 2,
            },
            label: {
              fontSize: 12,
              fontWeight: 'bold',
            },
          },
        },
      ],
    }
  }, [data])

  const bodyText6 = getBodyText6Style(theme)
  const legendItems = [
    { label: 'Good', color: '#079455' },
    { label: 'Critical', color: '#F79009' },
    { label: 'Needs Attention', color: '#D92D20' },
  ]

  const containerHeight = typeof height === 'number' ? `${height}px` : height

  const handleChartReady = (chart: echarts.ECharts) => {
    // Register click event
    chart.on('click', (params: unknown) => {
      const p = params as {
        data?: {
          stateId: string
          name: string
        }
      }
      if (p.data?.stateId && onStateClick) {
        onStateClick(p.data.stateId, p.data.name)
      }
    })

    // Register hover event
    chart.on('mouseover', (params: unknown) => {
      const p = params as {
        data?: {
          stateId: string
          name: string
        }
      }
      if (p.data?.stateId && onStateHover) {
        const stateData = data.find((d) => d.id === p.data?.stateId) ?? undefined
        if (stateData) {
          onStateHover(p.data.stateId, p.data.name, stateData)
        }
      }
    })
  }

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
        <EChartsWrapper option={option} height="100%" onChartReady={handleChartReady} />
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
                fontSize: bodyText6.fontSize,
                lineHeight: `${bodyText6.lineHeight}px`,
                fontWeight: bodyText6.fontWeight,
                color: bodyText6.color,
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
