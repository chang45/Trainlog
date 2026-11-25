// 敌人位置标记原型 —— 侦测核心
// 挂到玩家角色（或观战相机所在的 Pawn）上；敌方 Actor 打上 "Enemy" 标签即可。
// 判定流程与引擎无关，移植时保留 ScanTargets 里的几步即可。

#pragma once

#include "CoreMinimal.h"
#include "Components/ActorComponent.h"
#include "GameFramework/Actor.h"
#include "EnemyRadarComponent.generated.h"

class UCameraComponent;

UENUM(BlueprintType)
enum class EEnemyContactSource : uint8
{
    None UMETA(DisplayName = "无"),
    Sight UMETA(DisplayName = "目视"),
    Sound UMETA(DisplayName = "听声")
};

/** 一个敌人联系目标：当前是否可见，以及最后一次看到/听到的位置和时间 */
USTRUCT(BlueprintType)
struct FEnemyContact
{
    GENERATED_BODY()

    // 弱引用不暴露给蓝图，避免拖住已销毁的敌人；蓝图里用 GetTargetActor()
    TWeakObjectPtr<AActor> Target;

    UFUNCTION(BlueprintPure, Category = "Enemy Radar")
    AActor* GetTargetActor() const { return Target.Get(); }

    UPROPERTY(BlueprintReadOnly, Category = "Enemy Radar")
    FVector LastKnownLocation = FVector::ZeroVector;

    UPROPERTY(BlueprintReadOnly, Category = "Enemy Radar")
    float LastSeenTime = -1000.f;

    UPROPERTY(BlueprintReadOnly, Category = "Enemy Radar")
    float LastHeardTime = -1000.f;

    UPROPERTY(BlueprintReadOnly, Category = "Enemy Radar")
    bool bVisibleNow = false;

    UPROPERTY(BlueprintReadOnly, Category = "Enemy Radar")
    float Distance = 0.f;

    UFUNCTION(BlueprintPure, Category = "Enemy Radar")
    EEnemyContactSource GetSource() const
    {
        if (bVisibleNow)
        {
            return EEnemyContactSource::Sight;
        }
        if (LastSeenTime < 0.f && LastHeardTime < 0.f)
        {
            return EEnemyContactSource::None;
        }
        return LastSeenTime >= LastHeardTime ? EEnemyContactSource::Sight : EEnemyContactSource::Sound;
    }
};

DECLARE_DYNAMIC_MULTICAST_DELEGATE_OneParam(FEnemyContactAdded, const FEnemyContact&, Contact);

/**
 * 敌人侦测组件
 *
 * 每 ScanInterval 秒扫一次候选目标：
 *   1. 距离在 SightRange 内；
 *   2. 水平方向落在 SightHalfAngle 内（总视角 = 2 倍）；
 *   3. 视线无墙体遮挡（可关）；
 *   4. 距离在 HearingRadius 内且目标在移动 → 记为“声源”。
 * 看到/听到后把位置写入 LastKnownLocation，保留 MemoryDuration 秒，供 HUD 渐隐显示。
 */
UCLASS(ClassGroup = (Gameplay), meta = (BlueprintSpawnableComponent), DisplayName = "Enemy Radar")
class UEnemyRadarComponent : public UActorComponent
{
    GENERATED_BODY()

public:
    UEnemyRadarComponent();

    virtual void BeginPlay() override;
    virtual void TickComponent(float DeltaTime, ELevelTick TickType, FActorComponentTickFunction* ThisTickFunction) override;

    /** 敌方 Actor 的 Tag。留空则只用 SetTargets 手动登记的目标 */
    UPROPERTY(EditAnywhere, BlueprintReadWrite, Category = "Enemy Radar|Targets")
    FName EnemyActorTag = FName(TEXT("Enemy"));

    /** 视野距离（厘米） */
    UPROPERTY(EditAnywhere, BlueprintReadWrite, Category = "Enemy Radar|Detection", meta = (ClampMin = "0.0"))
    float SightRange = 3200.f;

    /** 视野半角（度）。110 度总视角填 55 */
    UPROPERTY(EditAnywhere, BlueprintReadWrite, Category = "Enemy Radar|Detection", meta = (ClampMin = "0.0", ClampMax = "180.0"))
    float SightHalfAngle = 55.f;

    /** 听声半径（厘米）。范围内的移动目标会以“声源”显示 */
    UPROPERTY(EditAnywhere, BlueprintReadWrite, Category = "Enemy Radar|Detection", meta = (ClampMin = "0.0"))
    float HearingRadius = 900.f;

    /** 目标速度低于该值视为静止，不触发听声（厘米/秒） */
    UPROPERTY(EditAnywhere, BlueprintReadWrite, Category = "Enemy Radar|Detection", meta = (ClampMin = "0.0"))
    float MinNoiseSpeed = 30.f;

    /** 看到/听到之后位置保留时长（秒），决定标记多久后消失 */
    UPROPERTY(EditAnywhere, BlueprintReadWrite, Category = "Enemy Radar|Detection", meta = (ClampMin = "0.1"))
    float MemoryDuration = 4.f;

    /** 是否要求视线无遮挡 */
    UPROPERTY(EditAnywhere, BlueprintReadWrite, Category = "Enemy Radar|Detection")
    bool bRequireLineOfSight = true;

    /** 遮挡检测使用的碰撞通道，墙体需要能挡住它 */
    UPROPERTY(EditAnywhere, BlueprintReadWrite, Category = "Enemy Radar|Detection")
    TEnumAsByte<ECollisionChannel> SightTraceChannel = ECC_Visibility;

    /** 用相机位置/朝向作为视野原点；关闭则用角色位置和朝向 */
    UPROPERTY(EditAnywhere, BlueprintReadWrite, Category = "Enemy Radar|Detection")
    bool bUseCameraForSight = true;

    /** 标记高度偏移（厘米），一般取角色身高附近 */
    UPROPERTY(EditAnywhere, BlueprintReadWrite, Category = "Enemy Radar|Marker")
    float MarkerHeight = 90.f;

    /** 扫描间隔（秒） */
    UPROPERTY(EditAnywhere, BlueprintReadWrite, Category = "Enemy Radar|Performance", meta = (ClampMin = "0.02"))
    float ScanInterval = 0.1f;

    /** 按 Tag 刷新目标列表的间隔（秒） */
    UPROPERTY(EditAnywhere, BlueprintReadWrite, Category = "Enemy Radar|Performance", meta = (ClampMin = "0.1"))
    float TargetRefreshInterval = 0.5f;

    /** 打开后在编辑器/开发包中画出视线和最后位置 */
    UPROPERTY(EditAnywhere, BlueprintReadWrite, Category = "Enemy Radar|Debug")
    bool bDrawDebug = false;

    /** 首次进入目视状态时广播，可用于播报音效、弹提示 */
    UPROPERTY(BlueprintAssignable, Category = "Enemy Radar")
    FEnemyContactAdded OnContactAdded;

    /** 手动指定侦测目标（不依赖 Tag），与 Tag 扫描二选一 */
    UFUNCTION(BlueprintCallable, Category = "Enemy Radar")
    void SetTargets(const TArray<AActor*>& InTargets);

    UFUNCTION(BlueprintCallable, Category = "Enemy Radar")
    void AddTarget(AActor* InTarget);

    UFUNCTION(BlueprintCallable, Category = "Enemy Radar")
    void ClearTargets();

    /** 当前所有联系目标（含渐隐中的） */
    UFUNCTION(BlueprintPure, Category = "Enemy Radar")
    TArray<FEnemyContact> GetContacts() const;

    const TArray<FEnemyContact>& GetContactsRef() const { return Contacts; }

    UFUNCTION(BlueprintPure, Category = "Enemy Radar")
    FVector GetEyeLocation();

    UFUNCTION(BlueprintPure, Category = "Enemy Radar")
    FVector GetEyeForward();

    UFUNCTION(BlueprintPure, Category = "Enemy Radar")
    float GetMemoryDuration() const { return MemoryDuration; }

private:
    void RefreshTargets();
    void ScanTargets();

    bool ComputeSeesActor(const AActor* Target, const FVector& Eye, const FVector& Forward, float& OutDistance) const;
    bool ComputeHearsActor(const AActor* Target, const FVector& Eye, float& OutDistance) const;

    TArray<TWeakObjectPtr<AActor>> ManualTargets;
    TArray<TWeakObjectPtr<AActor>> CachedTargets;
    TArray<FEnemyContact> Contacts;
    TWeakObjectPtr<UCameraComponent> CachedCamera;

    float ScanAccumulator = 0.f;
    float RefreshAccumulator = 0.f;
};
