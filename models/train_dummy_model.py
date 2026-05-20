     1|"""
     2|Dummy Fraud Detection Model
     3|LightGBM-like behavior simulated as a simple neural network in ONNX format
     4|
     5|Features (10 input features):
     6|  [0] amount_normalized     - 交易金额 (归一化)
     7|  [1] tx_count_24h          - 24小时内交易次数
     8|  [2] avg_amount_24h        - 24小时平均交易额
     9|  [3] geo_velocity          - 地理位置变化速度
    10|  [4] device_risk_score     - 设备风险分
    11|  [5] ip_reputation         - IP信誉分
    12|  [6] time_anomaly          - 时间异常指数
    13|  [7] amount_zscore         - 金额Z-Score
    14|  [8] merchant_diversity    - 商户多样性
    15|  [9] night_tx_ratio        - 夜间交易比例
    16|
    17|Output: fraud_score [0, 100]
    18|"""
    19|
    20|import numpy as np
    21|import onnx
    22|from onnx import helper, TensorProto
    23|import onnxruntime as ort
    24|
    25|# ============================================================
    26|# 1. Generate Synthetic Training Data
    27|# ============================================================
    28|np.random.seed(42)
    29|N_SAMPLES = 10000
    30|
    31|# Normal transactions
    32|normal = np.random.randn(N_SAMPLES // 2, 10) * 0.3
    33|normal[:, 0] = np.random.exponential(0.2, N_SAMPLES // 2)  # small amounts
    34|normal[:, 1] = np.random.poisson(3, N_SAMPLES // 2)         # few txns
    35|normal[:, 2] = normal[:, 0] * 0.8                           # avg ~ current
    36|normal[:, 3] = np.random.exponential(0.1, N_SAMPLES // 2)   # low geo velocity
    37|normal[:, 4] = np.random.beta(1, 5, N_SAMPLES // 2)         # low device risk
    38|normal[:, 5] = np.random.beta(8, 2, N_SAMPLES // 2)         # high IP reputation
    39|normal[:, 6] = np.random.exponential(0.1, N_SAMPLES // 2)   # low time anomaly
    40|normal[:, 7] = np.random.randn(N_SAMPLES // 2) * 0.5        # normal zscore
    41|normal[:, 8] = np.random.poisson(5, N_SAMPLES // 2)         # diverse merchants
    42|normal[:, 9] = np.random.beta(1, 8, N_SAMPLES // 2)         # low night ratio
    43|
    44|# Fraud transactions
    45|fraud = np.random.randn(N_SAMPLES // 2, 10) * 0.5
    46|fraud[:, 0] = np.random.exponential(5, N_SAMPLES // 2)      # large amounts
    47|fraud[:, 1] = np.random.poisson(15, N_SAMPLES // 2)         # many txns
    48|fraud[:, 2] = fraud[:, 0] * 0.2 + np.random.randn(N_SAMPLES // 2) * 2  # avg mismatch
    49|fraud[:, 3] = np.random.exponential(2, N_SAMPLES // 2)      # high geo velocity
    50|fraud[:, 4] = np.random.beta(5, 1, N_SAMPLES // 2)          # high device risk
    51|fraud[:, 5] = np.random.beta(1, 5, N_SAMPLES // 2)          # low IP reputation
    52|fraud[:, 6] = np.random.exponential(2, N_SAMPLES // 2)      # high time anomaly
    53|fraud[:, 7] = np.abs(np.random.randn(N_SAMPLES // 2)) * 3   # extreme zscore
    54|fraud[:, 8] = np.random.poisson(1, N_SAMPLES // 2)          # few merchants
    55|fraud[:, 9] = np.random.beta(5, 2, N_SAMPLES // 2)          # high night ratio
    56|
    57|X = np.vstack([normal, fraud])
    58|y = np.hstack([np.zeros(N_SAMPLES // 2), np.ones(N_SAMPLES // 2)])
    59|
    60|# Add bias term (intercept)
    61|X_with_bias = np.hstack([X, np.ones((N_SAMPLES, 1))])
    62|
    63|# ============================================================
    64|# 2. Train a simple Logistic Regression (mimics LightGBM)
    65|# ============================================================
    66|# Using closed-form solution: w = (X^T X)^{-1} X^T y
    67|# Then apply sigmoid for probability
    68|XtX = X_with_bias.T @ X_with_bias
    69|Xty = X_with_bias.T @ y
    70|w = np.linalg.solve(XtX, Xty)
    71|
    72|# Extract weights and bias
    73|weights = w[:10].astype(np.float32).reshape(1, 10)  # [1, 10]
    74|bias = np.array([w[10]], dtype=np.float32)           # [1]
    75|
    76|# ============================================================
    77|# 3. Build ONNX Graph manually
    78|# ============================================================
    79|# Graph structure:
    80|#   input: float[1, 10]  "features"
    81|#   MatMul(features, W) -> Gemm
    82|#   Add(bias) -> Add
    83|#   Sigmoid -> Sigmoid
    84|#   Scale(0, 100) -> Mul
    85|#   output: float[1, 1]  "fraud_score"
    86|
    87|# Create nodes
    88|nodes = [
    89|    # Gemm: Y = alpha * A * B + beta * C
    90|    helper.make_node(
    91|        'Gemm',
    92|        inputs=['features', 'weights', 'bias'],
    93|        outputs=['logits'],
    94|        name='LogisticLayer',
    95|        alpha=1.0,
    96|        beta=1.0,
    97|        transB=1,  # transpose weights so it's [10] -> [1]
    98|    ),
    99|    # Sigmoid activation (fraud probability)
   100|    helper.make_node(
   101|        'Sigmoid',
   102|        inputs=['logits'],
   103|        outputs=['probability'],
   104|        name='SigmoidActivation',
   105|    ),
   106|    # Scale from [0,1] to [0,100]
   107|    helper.make_node(
   108|        'Mul',
   109|        inputs=['probability', 'scale_100'],
   110|        outputs=['raw_score'],
   111|        name='ScaleTo100',
   112|    ),
   113|    # Round to 2 decimal places
   114|    helper.make_node(
   115|        'Clip',
   116|        inputs=['raw_score', 'min_score', 'max_score'],
   117|        outputs=['fraud_score'],
   118|        name='ClipScore',
   119|    ),
   120|]
   121|
   122|# Create graph input (ValueInfoProto) - dynamic batch size
   123|graph_input = [
   124|    helper.make_tensor_value_info(
   125|        'features', TensorProto.FLOAT,
   126|        shape=[None, 10]
   127|    )
   128|]
   129|
   130|# Create graph output (ValueInfoProto) - dynamic batch size
   131|graph_output = [
   132|    helper.make_tensor_value_info(
   133|        'fraud_score', TensorProto.FLOAT,
   134|        shape=[None, 1]
   135|    )
   136|]
   137|
   138|# Create initializers (weights, bias, constants)
   139|initializers = [
   140|    helper.make_tensor('weights', TensorProto.FLOAT, [1, 10], weights.flatten().tolist()),
   141|    helper.make_tensor('bias', TensorProto.FLOAT, [1], bias.tolist()),
   142|    helper.make_tensor('scale_100', TensorProto.FLOAT, [1], [100.0]),
   143|    helper.make_tensor('min_score', TensorProto.FLOAT, [1], [0.0]),
   144|    helper.make_tensor('max_score', TensorProto.FLOAT, [1], [100.0]),
   145|]
   146|
   147|# Create the graph
   148|graph = helper.make_graph(
   149|    nodes,
   150|    'fraud_detection_model',
   151|    graph_input,
   152|    graph_output,
   153|    initializers,
   154|)
   155|
   156|# Create the model with opset
   157|opset = [
   158|    helper.make_operatorsetid('', 18),  # ai.onnx opset 18
   159|]
   160|
   161|model = helper.make_model(graph, opset_imports=opset,
   162|                           producer_name='HermesAgent',
   163|                           producer_version='1.0',
   164|                           ir_version=8)
   165|
   166|# ============================================================
   167|# 4. Verify with onnxruntime
   168|# ============================================================
   169|model_bytes = model.SerializeToString()
   170|session = ort.InferenceSession(model_bytes)
   171|
   172|# Test with a normal transaction
   173|normal_input = np.array([[0.1, 2, 0.08, 0.05, 0.1, 0.9, 0.05, -0.2, 5, 0.02]], dtype=np.float32)
   174|result_normal = session.run(['fraud_score'], {'features': normal_input})[0]
   175|
   176|# Test with a fraud transaction
   177|fraud_input = np.array([[8.0, 20, 2.0, 3.5, 0.85, 0.15, 3.0, 6.0, 1, 0.8]], dtype=np.float32)
   178|result_fraud = session.run(['fraud_score'], {'features': fraud_input})[0]
   179|
   180|# Test batch scoring
   181|batch = np.vstack([normal_input, fraud_input])
   182|results = session.run(['fraud_score'], {'features': batch})[0]
   183|
   184|print("=" * 60)
   185|print("DUMMY FRAUD DETECTION MODEL - LIGHTGBM STYLE")
   186|print("=" * 60)
   187|print(f"\nModel input:  10 features (float32)")
   188|print(f"Model output: 1 fraud score [0-100]")
   189|print(f"ONNX version: {onnx.__version__}")
   190|print(f"Model IR ver: {model.ir_version}")
   191|print(f"Model size:   {len(model_bytes):,} bytes")
   192|print(f"\n--- Test Inference ---")
   193|print(f"Normal transaction score:  {result_normal[0][0]:.2f}/100")
   194|print(f"Fraud transaction score:   {result_fraud[0][0]:.2f}/100")
   195|print(f"\n--- Feature Importance ---")
   196|for i, name in enumerate(['amount_normalized', 'tx_count_24h', 'avg_amount_24h',
   197|                           'geo_velocity', 'device_risk_score', 'ip_reputation',
   198|                           'time_anomaly', 'amount_zscore', 'merchant_diversity',
   199|                           'night_tx_ratio']):
   200|    print(f"  {name:20s}: {weights[0][i]:+.4f}")
   201|
   202|print(f"\n--- Decision Thresholds ---")
   203|print(f"  Score < 30  → APPROVE")
   204|print(f"  Score 30-60 → PENDING (manual review)")
   205|print(f"  Score > 60  → REJECT")
   206|print(f"\n--- Batch Scoring (5 samples) ---")
   207|batch_test = np.random.randn(5, 10).astype(np.float32)
   208|batch_results = session.run(['fraud_score'], {'features': batch_test})[0]
   209|for i, score in enumerate(batch_results):
   210|    print(f"  Sample {i+1}: score={score[0]:.2f}")
   211|
   212|# Save model
   213|output_path = '/mnt/c/Users/alice/Desktop/fraud_model.onnx'
   214|with open(output_path, 'wb') as f:
   215|    f.write(model_bytes)
   216|print(f"\n✅ Model saved to: {output_path}")
   217|